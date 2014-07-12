package pl.h2p.pastebin;

import com.intellij.lifecycle.PeriodicalTasksCloser;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by eRIZ on 2014-07-11.
 */
@State(
    name = "ConfigurationState",
    storages = {
        @Storage(id = "dir", file = StoragePathMacros.PROJECT_CONFIG_DIR + "/pastebin.xml", scheme = StorageScheme.DIRECTORY_BASED)
    }
)
public class ConfigurationState implements PersistentStateComponent<ConfigurationState> {

    public String url = "";
    public String apiKey = "";

    public ConfigurationState(){
        //
    }

    @Nullable
    @Override
    public ConfigurationState getState() {
        return this;
    }

    @Override
    public void loadState(ConfigurationState configurationState) {
        XmlSerializerUtil.copyBean(configurationState, this);
    }

    public static ConfigurationState getInstance(Project project)
    {
        return PeriodicalTasksCloser.getInstance().safeGetService(project, ConfigurationState.class);
    }

    public boolean isUrlValid(){
        CloseableHttpClient client = HttpClients.createDefault();

        try {
            URIBuilder uri = new URIBuilder(getUrl());
            HttpGet get = new HttpGet(uri.build());

            HttpResponse r = client.execute(get);
            HttpEntity result = r.getEntity();
            String data = EntityUtils.toString(result);

            String validator = "authenticity_token";

            if (data.indexOf(validator)<0) {
                return false;
            }

            return true;
        } catch (URISyntaxException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

    }

    public boolean isApiKeyValid(){
        CloseableHttpClient client = HttpClients.createDefault();

        try {
            URIBuilder uri = new URIBuilder(getUrl());

            String path = uri.getPath();

            StringBuilder newPath = new StringBuilder(path);

            if(path.length()==0 || path.substring(path.length()-1)!="/"){
                newPath.append("/");
            }

            newPath.append("issues.json");

            uri.setPath(newPath.toString());
            uri.addParameter("key", apiKey);

            HttpGet get = new HttpGet(uri.build());

            HttpResponse r = client.execute(get);
            if(r.getStatusLine().getStatusCode()!=200){
                return false;
            }

            return true;
        } catch (URISyntaxException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
