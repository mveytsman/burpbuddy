package burp;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import org.apache.commons.codec.binary.Base64;
import com.google.gson.Gson;
import static spark.Spark.*;

public class ApiServer {


    public ApiServer(String ip, int port, IBurpExtenderCallbacks callbacks) {

        IExtensionHelpers helpers = callbacks.getHelpers();
        setPort(port);
        setIpAddress(ip);

        Gson gson = new Gson();

        get("/scope/:url", (request, response) -> {
            try {
                URL url = new URL(new String(Base64.decodeBase64(request.params("url"))));
                if (callbacks.isInScope(url)) {
                    response.status(200);
                    return "";
                } else {
                    response.status(404);
                    return "";
                }
            } catch (MalformedURLException e) {
                response.status(400);
                return e.getMessage();
            }
        });

        post("/scope", (request, response) -> {
            try {
                BURLMessage message = gson.fromJson(request.body(), BURLMessage.class);
                callbacks.includeInScope(new URL(message.url));
                response.status(201);
                return gson.toJson(message);
            } catch (MalformedURLException e) {
                response.status(400);
                return e.getMessage();
            }
        });

        delete("/scope/:url", (request, response) -> {
            try {
                URL url = new URL(new String(Base64.decodeBase64(request.params("url"))));
                callbacks.excludeFromScope(url);
                response.status(200);
                return "";
            } catch (MalformedURLException e) {
                response.status(400);
                return e.getMessage();
            }
        });

        get("/scanissues", (request, response) -> {
            IScanIssue[] rawIssues = callbacks.getScanIssues("");
            List<BScanIssue> issues = new ArrayList<>();
            for (IScanIssue issue : rawIssues) {
                issues.add(BScanIssueFactory.create(issue, callbacks));
            }
            return gson.toJson(issues);
        });

        get("/scanissues/:url", (request, response) -> {
            byte[] encodedBytes = Base64.decodeBase64(request.params("url"));
            IScanIssue[] rawIssues =  callbacks.getScanIssues(new String(encodedBytes));
            List<BScanIssue> issues = new ArrayList<>();
            for (IScanIssue issue : rawIssues) {
                issues.add(BScanIssueFactory.create(issue, callbacks));
            }
            return gson.toJson(issues);
        });

        post("/scanissues", (request, response) -> {
            BScanIssue issue = gson.fromJson(request.body(), BScanIssue.class);
            callbacks.addScanIssue(issue);
            response.status(201);
            return gson.toJson(issue);
        });

        post("/spider", (request, response) -> {
            BURLMessage message = gson.fromJson(request.body(), BURLMessage.class);
            try {
                callbacks.sendToSpider(new URL(message.url));
                response.status(200);
                return "";
            } catch (MalformedURLException e) {
                response.status(400);
                return e.getMessage();
            }
        });

        get("/jar", (request, response) -> {
            List<BCookie> cookies = new ArrayList<>();
            for (ICookie burpCookie: callbacks.getCookieJarContents()) {
                BCookie cookie = new BCookie();
                cookie.experation = burpCookie.getExpiration();
                cookie.domain = burpCookie.getDomain();
                cookie.name = burpCookie.getName();
                cookie.value = burpCookie.getValue();
                cookies.add(cookie);
            }
            return gson.toJson(cookies);
        });

    }

    public void stopServer() {
        stop();
    }

}