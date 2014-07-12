package pl.h2p.pastebin;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.intellij.notification.Notification;

import com.intellij.notification.NotificationListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by eRIZ on 2014-07-11.
 */
public class PastebinAction extends AnAction {

    protected String url = "";
    protected String apiKey = "";
    protected boolean lastValidationResult = false;

    protected boolean validateSettings(ConfigurationState conf){

        if(url.equals(conf.getUrl()) && apiKey.equals(conf.getApiKey())){
            return lastValidationResult;
        }

        boolean urlValid = conf.isUrlValid();
        boolean apiKeyValid = conf.isApiKeyValid();

        if(urlValid && apiKeyValid){
            lastValidationResult = true;
        }else{
            lastValidationResult = false;
        }

        return lastValidationResult;
    }

    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }

        SelectionModel sm = editor.getSelectionModel();
        String text = sm.getSelectedText();

        if(text == null){
            String content = "Make some selection first";

            Notification n = new Notification("pastebin", "Pastebin", content, NotificationType.WARNING);
            Notifications.Bus.notify(n);
            return;
        }

        ConfigurationState conf = ConfigurationState.getInstance(project);

        if(!validateSettings(conf)){
            String content = "Cannot reach Redmine. Check <a href=\"\">Settings</a>";

            Notification n = new Notification("pastebin", "Pastebin", content, NotificationType.WARNING, new NotificationListener() {
                @Override
                public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, PastebinSettings.settingName);
                    notification.expire();
                }
            });
            Notifications.Bus.notify(n);
            return;
        }

        URIBuilder uri;

        try {
            uri = new URIBuilder(conf.getUrl());
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
            return;
        }

        String path = uri.getPath();

        StringBuilder newPath = new StringBuilder(path);

        if(path.length()==0 || path.substring(path.length()-1)!="/"){
            newPath.append("/");
        }

        newPath.append("pastes/add/api");

        uri.setPath(newPath.toString());
        uri.addParameter("key", conf.getApiKey());

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post;
        try {
            post = new HttpPost(uri.build());
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
            return;
        }

        StringEntity payload = new StringEntity(text, "UTF-8");

        post.setEntity(payload);
        try {
            HttpResponse r = client.execute(post);

            HttpEntity result = r.getEntity();
            String data = EntityUtils.toString(result);

            String content = getBalloonContent(data);

            NotificationListener listener = new NotificationListener() {
                @Override
                public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {

                    String href = hyperlinkEvent.getURL().toExternalForm();

                    String action = "";

                    try {
                        URIBuilder u = new URIBuilder(href);
                        action = u.getFragment();
                    } catch (URISyntaxException e1) {
                        e1.printStackTrace();
                    }

                    if(action!=null && action.equals("copy")){
                        CopyPasteManager.getInstance().setContents(new StringSelection(href));
                    }else{
                        BrowserUtil.browse(href);
                    }

                    notification.hideBalloon();
                }
            };

            Notification n = new Notification("pastebin", "Pastebin", content, NotificationType.INFORMATION, listener);

            Notifications.Bus.notify(n);
        } catch (IOException e1) {
            e1.printStackTrace();
        }


    }

    protected String getBalloonContent(String link){
        StringBuilder content = new StringBuilder();

        content.append("Paste successfully uploaded!<br><br>");
        content.append("Show: <a href=\"");
        content.append(link);
        content.append("\">");
        content.append(link);
        content.append("</a> (<a href=\"" + link +
                "#copy\">copy</a>)<br>");

        content.append("Raw: <a href=\"");
        content.append(link);
        content.append("/raw\">");
        content.append(link);
        content.append("/raw</a> (<a href=\"" + link +
                "/raw#copy\">copy</a>)");

        return content.toString();
    }
}
