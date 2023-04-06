/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.http.pipeline;


import okhttp3.Request;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.*;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

public class HttpDataSource implements DataSource {
    private String name;
    private HttpRequestParams params;
    private String requestId;
    private Monitor monitor;
    private EdcHttpClient httpClient;

    @Override
    public Stream<Part> openPartStream() {
        return Stream.of(getPart());
    }

    private HttpPart getPart() {
        // do head request and try to get content length for data
        int size = -1;
        Request headRequest = params.toHeadRequest();
        monitor.debug(() -> "HttpDataSource sends head request: " + headRequest.toString());
        try (var response = httpClient.execute(headRequest)) {
            if (response.isSuccessful()) {
                size = response.header("Content-Length") != null ? Integer.parseInt(response.header("Content-Length")) : -1;
            } else {
                monitor.debug(() -> format("Received code getting content-length for HTTP data with head request %s: %s - %s.", requestId, response.code(), response.message()));
            }
        } catch (IOException e) {
            monitor.debug(() -> "HttpDataSource cannot get content-length for HTTP data with head request.");
        }

        // construct Http Part
        var request = params.toRequest();
        return new HttpPart(name, request, size, requestId, monitor, httpClient);
    }

    private HttpDataSource() {
    }

    public static class Builder {
        private final HttpDataSource dataSource;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder params(HttpRequestParams params) {
            dataSource.params = params;
            return this;
        }

        public Builder name(String name) {
            dataSource.name = name;
            return this;
        }

        public Builder requestId(String requestId) {
            dataSource.requestId = requestId;
            return this;
        }

        public Builder httpClient(EdcHttpClient httpClient) {
            dataSource.httpClient = httpClient;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSource.monitor = monitor;
            return this;
        }

        public HttpDataSource build() {
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.httpClient, "httpClient");
            Objects.requireNonNull(dataSource.monitor, "monitor");
            return dataSource;
        }

        private Builder() {
            dataSource = new HttpDataSource();
        }
    }


    private static class HttpPart implements Part {
        private final String name;
        private final Request request;
        private final int size;
        private final String requestId;
        private final Monitor monitor;
        private final EdcHttpClient httpClient;


        public static final double MAX_NON_CHUNKED_TRANSFER_SIZE = 0xF00000; // 15MB
        public static final double CHUNKED_TRANSFER_CHUNK_SIZE = 0xA00000; // 10MB

        HttpPart(String name, Request request, int size, String requestId, Monitor monitor, EdcHttpClient httpClient) {
            this.name = name;
            this.request = request;
            this.size = size;
            this.requestId = requestId;
            this.monitor = monitor;
            this.httpClient = httpClient;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public InputStream openStream() {
            if (size == -1 || size < MAX_NON_CHUNKED_TRANSFER_SIZE) {
                monitor.debug(() -> "HttpDataSource sends request: " + request.toString());
                return doRequest(request, (int) MAX_NON_CHUNKED_TRANSFER_SIZE);
            } else {
                // make initial request to get first chunk
                monitor.debug(() -> format("HttpDataSource sends multiple chunked requests with chunksize %d: %s", (int) CHUNKED_TRANSFER_CHUNK_SIZE, request.toString()));
                InputStream stream = new InputStream() {
                    private int chunkCount = 0;
                    private int i = 0;
                    private int lastRead = 0;
                    private byte[] buffer = new byte[(int) CHUNKED_TRANSFER_CHUNK_SIZE];
                    private InputStream chunk = null;

                    // initialize first chunk
                    {
                        var req = request.newBuilder()
                                .header("Range", "bytes=" + 0 + "-" + ((int) CHUNKED_TRANSFER_CHUNK_SIZE - 1))
                                .build();
                        chunk = doRequest(req, (int) CHUNKED_TRANSFER_CHUNK_SIZE);
                    }

                    @Override
                    public int read() throws IOException {
                        lastRead = chunk.read();
                        i++;

                        // fetch data for next chunk
                        if (i == CHUNKED_TRANSFER_CHUNK_SIZE) {
                            chunkCount++;
                            int start = chunkCount * (int) CHUNKED_TRANSFER_CHUNK_SIZE;
                            int end = (chunkCount + 1) * (int) CHUNKED_TRANSFER_CHUNK_SIZE - 1;

                            if (end >= size) {
                                end = size;
                            }

                            if (start >= end) {
                                return -1;
                            }

                            var req = request.newBuilder()
                                .header("Range", "bytes=" + start + "-" + end)
                                .build();

                            chunk = doRequest(req, (int) CHUNKED_TRANSFER_CHUNK_SIZE);

                            i = 0;
                        }

                        return lastRead;
                    }
                };

                return new BufferedInputStream(stream, (int) CHUNKED_TRANSFER_CHUNK_SIZE);
            }
        }

        private ByteArrayInputStream doRequest(Request request, int maxContentLength) {
            try (var response = httpClient.execute(request)) {
                if (response.isSuccessful()) {
                    var body = response.body();
                    if (body == null) {
                        throw new EdcException(format("Received empty response body transferring HTTP data for request %s: %s", requestId, response.code()));
                    }

                    var source = body.source();
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead = 0;
                    int totalBytesRead = 0;

                    while ((bytesRead = source.read(buffer)) != -1) {
                        totalBytesRead += bytesRead;
                        if (totalBytesRead > maxContentLength) {
                            throw new EdcException(format("The body exceeded the size limit (%d) for the for request %s", maxContentLength, requestId));
                        }
                        output.write(buffer, 0, bytesRead);
                    }

                    if (totalBytesRead == 0) {
                        throw new EdcException(format("Received empty response body transferring HTTP data for request %s: %s", requestId, response.code()));
                    }

                    return new ByteArrayInputStream(output.toByteArray());
                } else {
                    var bytesBody = response.body().bytes();
                    throw new EdcException(format("Received code transferring HTTP data for request %s: %s - %s.", requestId, response.code(), response.message(), bytesBody));
                }
            } catch (IOException e) {
                throw new EdcException(e);
            }
        }

    }
}
