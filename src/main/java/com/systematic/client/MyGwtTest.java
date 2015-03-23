package com.systematic.client;

import com.google.gwt.user.client.ui.*;
import com.systematic.shared.FieldVerifier;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.gwtopenmaps.openlayers.client.*;
import org.gwtopenmaps.openlayers.client.control.LayerSwitcher;
import org.gwtopenmaps.openlayers.client.control.OverviewMap;
import org.gwtopenmaps.openlayers.client.control.ScaleLine;
import org.gwtopenmaps.openlayers.client.feature.VectorFeature;
import org.gwtopenmaps.openlayers.client.geometry.Point;
import org.gwtopenmaps.openlayers.client.layer.OSM;
import org.gwtopenmaps.openlayers.client.layer.Vector;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class MyGwtTest implements EntryPoint {
  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while "
      + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  /**
   * Create a remote service proxy to talk to the server-side Greeting service.
   */
  private final GreetingServiceAsync greetingService = GWT.create(GreetingService.class);

  private final Messages messages = GWT.create(Messages.class);

  private final Projection DEFAULT_PROJECTION = new Projection("EPSG:4326");

    /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    final Button sendButton = new Button( messages.sendButton() );
    final TextBox nameField = new TextBox();
    nameField.setText( messages.nameField() );
    final Label errorLabel = new Label();

    // We can add style names to widgets
    sendButton.addStyleName("sendButton");

    // Add the nameField and sendButton to the RootPanel
    // Use RootPanel.get() to get the entire body element
    RootPanel.get("nameFieldContainer").add(nameField);
    RootPanel.get("nameFieldContainer").add(getMapWidget());
    RootPanel.get("sendButtonContainer").add(sendButton);
    RootPanel.get("errorLabelContainer").add(errorLabel);

    // Focus the cursor on the name field when the app loads
    nameField.setFocus(true);
    nameField.selectAll();

    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Remote Procedure Call");
    dialogBox.setAnimationEnabled(true);
    final Button closeButton = new Button("Close");
    // We can set the id of a widget by accessing its Element
    closeButton.getElement().setId("closeButton");
    final Label textToServerLabel = new Label();
    final HTML serverResponseLabel = new HTML();
    VerticalPanel dialogVPanel = new VerticalPanel();
    dialogVPanel.addStyleName("dialogVPanel");
    dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
    dialogVPanel.add(textToServerLabel);
    dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
    dialogVPanel.add(serverResponseLabel);
    dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
    dialogVPanel.add(closeButton);
    dialogBox.setWidget(dialogVPanel);

    // Add a handler to close the DialogBox
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
        sendButton.setEnabled(true);
        sendButton.setFocus(true);
      }
    });

    // Create a handler for the sendButton and nameField
    class MyHandler implements ClickHandler, KeyUpHandler {
      /**
       * Fired when the user clicks on the sendButton.
       */
      public void onClick(ClickEvent event) {
        sendNameToServer();
      }

      /**
       * Fired when the user types in the nameField.
       */
      public void onKeyUp(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          sendNameToServer();
        }
      }

      /**
       * Send the name from the nameField to the server and wait for a response.
       */
      private void sendNameToServer() {
        // First, we validate the input.
        errorLabel.setText("");
        String textToServer = nameField.getText();
        if (!FieldVerifier.isValidName(textToServer)) {
          errorLabel.setText("Please enter at least four characters (yeay)");
          return;
        }

        // Then, we send the input to the server.
        sendButton.setEnabled(false);
        textToServerLabel.setText(textToServer);
        serverResponseLabel.setText("");
        greetingService.greetServer(textToServer, new AsyncCallback<String>() {
          public void onFailure(Throwable caught) {
            // Show the RPC error message to the user
            dialogBox.setText("Remote Procedure Call - Failure");
            serverResponseLabel.addStyleName("serverResponseLabelError");
            serverResponseLabel.setHTML(SERVER_ERROR);
            dialogBox.center();
            closeButton.setFocus(true);
          }

          public void onSuccess(String result) {
            dialogBox.setText("Remote Procedure Call");
            serverResponseLabel.removeStyleName("serverResponseLabelError");
            serverResponseLabel.setHTML(result);
            dialogBox.center();
            closeButton.setFocus(true);
          }
        });
      }
    }

    // Add a handler to send the name to the server
    MyHandler handler = new MyHandler();
    sendButton.addClickHandler(handler);
    nameField.addKeyUpHandler(handler);
  }

    private Widget getMapWidget() {
        MapOptions mapOptions = new MapOptions();
        mapOptions.setNumZoomLevels(25);
        MapWidget mapWidget = new MapWidget("500px", "500px", mapOptions);
        OSM osm_1 = OSM.Mapnik("My Map Layer (Mapnik)");
        OSM osm_2 = OSM.CycleMap("Other layer (CycleMap)");
        osm_1.setIsBaseLayer(true);
        osm_2.setIsBaseLayer(true);
        Map map = mapWidget.getMap();
        map.addLayer(osm_1);
        map.addLayer(osm_2);

        // Lets add some default controls to the map
        map.addControl(new LayerSwitcher()); // + sign in the upperright corner to display the layer switcher
        map.addControl(new OverviewMap()); // + sign in the lowerright to display the overviewmap
        map.addControl(new ScaleLine()); // Display the scaleline

        // Create a marker layer to the current location marker
        final Vector markerLayer = new Vector("Marker layer");
        map.addLayer(markerLayer);

        LonLat lonlat = new LonLat(11.4, 55.35); // Map center location
        lonlat.transform(DEFAULT_PROJECTION.getProjectionCode(), map.getProjection());
        map.setCenter(lonlat, 10); // Map zoom level is 10

        LonLat where = new LonLat(11.5, 55.4); // Marker location
        VectorFeature marker = getMapMarker(where, new Projection(map.getProjection()));
        markerLayer.addFeature(marker);
        return mapWidget;
    }

    private VectorFeature getMapMarker(final LonLat where, final Projection projection) {

        Style pointStyle = new Style();
        pointStyle.setFillColor("yellow");
        pointStyle.setStrokeColor("red");
        pointStyle.setStrokeWidth(1);
        pointStyle.setFillOpacity(0.5);
        //pointStyle.setPointRadius(25d);

        //org.gwtopenmaps.openlayers.client.geometry.Curve


        // Curve curve = new Curve(getPoints(where));
        // curve.transform(DEFAULT_PROJECTION, projection); // transform point to OSM coordinate system
        // final VectorFeature curveFeature = new VectorFeature(curve, pointStyle);

        final Point point = new Point(where.lon(), where.lat());
        point.transform(DEFAULT_PROJECTION, projection); // transform point to OSM coordinate system
        final VectorFeature pointFeature = new VectorFeature(point, pointStyle);

        return pointFeature;
    }

    private Point[] getPoints(final LonLat where) {
        Point[] points = new Point[3];
        points[0] = new Point(where.lon(), where.lat()-2);
        points[1] = new Point(where.lon()-2, where.lat());
        points[2] = new Point(where.lon(), where.lat()+2);
        return points;
    }

}
