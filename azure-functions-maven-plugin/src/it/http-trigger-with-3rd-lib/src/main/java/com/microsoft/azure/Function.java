package eu.hanskruse;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.microsoft.azure.serverless.functions.annotation.*;
import com.microsoft.azure.serverless.functions.*;
import com.microsoft.itlib.AbstractSigner;
import com.microsoft.itlib.ConcreteSigner;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    @FunctionName("thirdparty")
    public HttpResponseMessage<String> hello(
            @HttpTrigger(name = "req", methods = {"get", "post"}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        final Logger logger = context.getLogger();
        final AbstractSigner signer = new ConcreteSigner();
        final int signatureLength= signer.getSignature(null).length;
        logger.log(Level.SEVERE,"Signature length: " + signatureLength);

        // Parse query parameter
        String name = request.getQueryParameters().get("name");
        return request.createResponse(200, "Hello, " + name);

    }
}
