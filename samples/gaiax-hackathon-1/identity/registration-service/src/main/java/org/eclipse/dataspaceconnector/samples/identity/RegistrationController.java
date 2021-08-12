package org.eclipse.dataspaceconnector.samples.identity;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.spi.iam.ObjectStore;
import org.eclipse.dataspaceconnector.spi.iam.RegistrationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/identity")
public class RegistrationController implements RegistrationService {
    private final Monitor monitor;
    private final ObjectStore<DidDocument> didDocumentStore;

    public RegistrationController(Monitor monitor, ObjectStore<DidDocument> didDocumentStore) {
        this.monitor = monitor;
        this.didDocumentStore = didDocumentStore;
    }

    @GET
    @Path("{paginationOffset}")
    public Response getDids(@PathParam("paginationOffset") String offset) {
        monitor.info("Fetching all DIDs");
        List<DidDocument> allDids;
        if (StringUtils.isNullOrBlank(offset)) {
            allDids = getAllDids(100);
        } else {
            allDids = getDidsWithOffset(offset);
        }

        return Response.ok(allDids).build();
    }

    private List<DidDocument> getDidsWithOffset(String offset) {
        return didDocumentStore.getAfter(offset);
    }

    private List<DidDocument> getAllDids(int maxNumber) {
        return didDocumentStore.getAll(maxNumber);
    }
}