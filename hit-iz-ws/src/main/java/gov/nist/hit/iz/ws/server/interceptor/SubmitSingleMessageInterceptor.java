package gov.nist.hit.iz.ws.server.interceptor;

import gov.nist.auth.hit.core.repo.TransportConfigRepository;
import gov.nist.hit.core.domain.Transaction;
import gov.nist.hit.core.domain.util.XmlUtil;
import gov.nist.hit.core.service.TransactionService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SubmitSingleMessageInterceptor implements EndpointInterceptor {

  static final Logger logger = LoggerFactory.getLogger(SubmitSingleMessageInterceptor.class);

  @Autowired
  private TransactionService transactionService;

  @Autowired
  private TransportConfigRepository transportConfigRepository;

  @Override
  public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception {
    // SoapMessage message = (SoapMessage) messageContext.getRequest();
    // message.setSoapAction(IZWSConstant.SUBMITSINGLEMESSAGE_SOAP_ACTION);
    return true;
  }

  @Override
  public boolean handleResponse(MessageContext messageContext, Object endpoint) throws Exception {
    addMessages(messageContext);
    return true;
  }

  @Override
  public boolean handleFault(MessageContext messageContext, Object endpoint) throws Exception {
    addMessages(messageContext);
    return true;
  }

  @Transactional()
  private void addMessages(String request, String response) {
    String username = getUsername(request);
    String password = getPassword(request);
    String facilityID = getFacilityID(request);
    try {
      Map<String, String> properties = getProperties(username, password, facilityID);
      Transaction transaction = transactionService.findOneByProperties(properties);
      if (transaction == null) {
        transaction = new Transaction();
        transaction.setProperties(properties);
      }
      transaction.setIncoming(XmlUtil.prettyPrint(request));
      transaction.setOutgoing(XmlUtil.prettyPrint(response));
      transactionService.save(transaction);
    } catch (Exception e) {
      logger.error("Failed to persist messages for username= " + username);
    }
  }



  private void addMessages(MessageContext messageContext) {
    addMessages(toString(messageContext.getRequest()), toString(messageContext.getResponse()));
  }

  @Override
  public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex)
      throws Exception {
    // TODO Auto-generated method stub

  }

  private String toString(final WebServiceMessage message) {
    try {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      message.writeTo(outputStream);
      return new String(outputStream.toByteArray());
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String getFacilityID(String requestContent) {
    return getElementContent(requestContent, "facilityID");
  }

  public String getUsername(String requestContent) {
    return getElementContent(requestContent, "username");
  }

  public String getPassword(String requestContent) {
    return getElementContent(requestContent, "password");
  }

  public String getElementContent(String requestContent, String element) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc =
          builder
              .parse(new InputSource(new ByteArrayInputStream(requestContent.getBytes("utf-8"))));
      XPathFactory xPathfactory = XPathFactory.newInstance();
      XPath xpath = xPathfactory.newXPath();
      XPathExpression expr =
          xpath
              .compile("//*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='submitSingleMessage']/*[local-name()='"
                  + element + "']/text()");
      String result = (String) expr.evaluate(doc, XPathConstants.STRING);
      return result;
    } catch (XPathExpressionException e) {
      logger.error("Failed to get submitSingleMessage " + element + " value");
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      logger.error("Failed to get submitSingleMessage " + element + " value");
      e.printStackTrace();
    } catch (IOException e) {
      logger.error("Failed to get submitSingleMessage " + element + " value");
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      logger.error("Failed to get submitSingleMessage " + element + " value");
      e.printStackTrace();
    }
    return null;
  }

  private Map<String, String> getProperties(String username, String password, String facilityID) {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("username", username);
    properties.put("password", password);
    properties.put("facilityID", facilityID);
    return properties;
  }

}
