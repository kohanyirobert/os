package com.github.kohanyirobert.os;

import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayOutputStream;

import static javax.xml.xpath.XPathConstants.NODE;

public final class Main {

    private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();
    private static final XPath X_PATH = X_PATH_FACTORY.newXPath();
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final DocumentBuilder DOCUMENT_BUILDER;
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
    private static final Transformer TRANSFORMER;

    static {
        try {
            DOCUMENT_BUILDER = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
            TRANSFORMER = TRANSFORMER_FACTORY.newTransformer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create()
                .build();
        String token = logIn(client);
        String link = searchSubtitles(client, token, args[0], args[1]);
        System.out.println(download(client, link));
    }

    private static String logIn(CloseableHttpClient client) throws Exception {
        HttpPost request = new HttpPost("http://api.opensubtitles.org/xml-rpc");
        request.setEntity(new InputStreamEntity(Main.class.getResourceAsStream("/LogIn.xml")));
        CloseableHttpResponse response = client.execute(request);
        return X_PATH.evaluate("(.//string)[1]/text()", DOCUMENT_BUILDER.parse(response.getEntity().getContent()));
    }

    private static String searchSubtitles(CloseableHttpClient client, String token, String languageId, String movieHash) throws Exception {
        Document document = DOCUMENT_BUILDER.parse(Main.class.getResourceAsStream("/SearchSubtitles.xml"));
        setNodeTextContent("(./methodCall/params/param)[1]/value/string", document, token);
        setNodeTextContent("((./methodCall/params/param)[2]/value/array/data/value/struct/member)[1]/value/string", document, languageId);
        setNodeTextContent("((./methodCall/params/param)[2]/value/array/data/value/struct/member)[2]/value/string", document, movieHash);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TRANSFORMER.transform(new DOMSource(document), new StreamResult(baos));
        HttpPost request = new HttpPost("http://api.opensubtitles.org/xml-rpc");
        request.setEntity(new ByteArrayEntity(baos.toByteArray()));
        CloseableHttpResponse response = client.execute(request);
        return X_PATH.evaluate("(.//name[text()='SubDownloadLink']/following-sibling::*//string)[1]/text()",
                DOCUMENT_BUILDER.parse(response.getEntity().getContent()));
    }

    private static String download(CloseableHttpClient client, String link) throws Exception {
        HttpPost request = new HttpPost(link);
        CloseableHttpResponse response = client.execute(request);
        return EntityUtils.toString(new GzipDecompressingEntity(response.getEntity()));
    }


    private static void setNodeTextContent(String xPath, Document document, String token) throws Exception {
        ((Node) X_PATH.evaluate(xPath, document, NODE)).setTextContent(token);
    }
}
