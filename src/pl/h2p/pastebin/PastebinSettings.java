package pl.h2p.pastebin;

import com.intellij.openapi.components.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by eRIZ on 2014-07-11.
 */


public class PastebinSettings implements Configurable, ProjectComponent{
    private JPanel panel1;
    private JTextField urlTextField;
    private JTextField apiKeyTextField;
    private JButton testButton;
    private JLabel entrypointError;
    private JLabel apikeyError;
    private JLabel status;

    protected ConfigurationState conf;

    protected boolean changed = false;

    public final static String settingName = "Redmine Pastebin";

    public PastebinSettings(Project project){
        conf = ConfigurationState.getInstance(project);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return settingName;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        entrypointError.setVisible(false);
        apikeyError.setVisible(false);
        status.setVisible(false);

        urlTextField.setText(conf.getUrl());

        urlTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                changed(documentEvent);
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                changed(documentEvent);
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                changed(documentEvent);
            }

            protected void changed(DocumentEvent e){
                Document d = e.getDocument();
                try {
                    String content = d.getText(0, d.getEndPosition().getOffset()).trim();
                    conf.setUrl(content);
                    changed = true;
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }
        });

        apiKeyTextField.setText(conf.getApiKey());

        apiKeyTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                changed(documentEvent);
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                changed(documentEvent);
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                changed(documentEvent);
            }

            protected void changed(DocumentEvent e){
                Document d = e.getDocument();
                try {
                    String content = d.getText(0, d.getEndPosition().getOffset()).trim();
                    conf.setApiKey(content);
                    changed = true;
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }
        });

        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                status.setVisible(false);

                boolean urlValid = conf.isUrlValid();
                boolean apikeyValid = conf.isApiKeyValid();

                entrypointError.setVisible(
                    !urlValid
                );
                apikeyError.setVisible(
                    !apikeyValid
                );

                if(urlValid && apikeyValid){

                    CloseableHttpClient client = HttpClients.createDefault();

                    boolean result = true;

                    try {
                        URIBuilder uri = new URIBuilder(conf.getUrl());

                        String path = uri.getPath();

                        StringBuilder newPath = new StringBuilder(path);

                        if(path.length()==0 || path.substring(path.length()-1)!="/"){
                            newPath.append("/");
                        }

                        newPath.append("pastes");

                        uri.setPath(newPath.toString());
                        uri.addParameter("key", conf.getApiKey());

                        HttpGet get = new HttpGet(uri.build());

                        HttpResponse r = client.execute(get);
                        if(r.getStatusLine().getStatusCode()!=200){
                            result = false;
                        }
                    } catch (URISyntaxException e) {
                        result = false;
                    } catch (IOException e) {
                        result = false;
                    }

                    if(result){
                        status.setText("Plugin found!");
                    }else{
                        status.setText("Plugin NOT found!");
                    }

                    status.setVisible(true);

                }
            }
        });

        return panel1;
    }

    @Override
    public boolean isModified() {
        return changed;
    }

    @Override
    public void apply() throws ConfigurationException {

    }

    @Override
    public void reset() {

    }

    @Override
    public void disposeUIResources() {

    }

    @Override
    public void projectOpened() {

    }

    @Override
    public void projectClosed() {

    }

    @Override
    public void initComponent() {

    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return null;
    }
}
