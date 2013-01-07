package nxtbeefx;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Controller for NXTBeeFX
 * -Define actions for buttons to control NXT
 * -Start/Stops serial reading from NXT
 * -Sends to GPIO alarm status
 * 
 * @author José Pereda Llamas
 * Created on 27-dic-2012 - 19:32:10
 */
public class NXTBeeFXController implements Initializable {
    
    public static final String START    = "S";
    public static final String QUIT     = "Q";
    public static final String STOP     = "P";
    public static final String AUTO     = "A";
    public static final String MANUAL   = "M";
    public static final String LEFT     = "L";
    public static final String RIGHT    = "R";
    public static final String FORWARD  = "F";
    public static final String BACKWARD = "B";
    public static final String SPEED_UP = "V";
    public static final String SPEED_DW = "W";
    
    private static final int ACK_STOP    = 0;
    private static final int ACK_FORWARD = 1;
    private static final int ACK_WALL    = 2;
    private static final int ACK_MANUAL  = 3;    
    
    private Serial serial;
    private Service serviceSerial;
    
    private ControlGpio GPIO;
    
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Button btnManual;
    @FXML private Button btnQuit;
    
    @FXML private HBox panManual;

    @FXML private Button btnFowr;
    @FXML private Button btnBack;
    @FXML private Button btnLeft;
    @FXML private Button btnRight;
    @FXML private Button btnVelUp;
    @FXML private Button btnVelDown;
        
    @FXML private Label lblStatus;
    private IntegerProperty iStatus;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        btnStart.setDisable(false);
        btnStop.setDisable(true);
        btnManual.setDisable(true);
        btnQuit.setDisable(true);        
        panManual.setDisable(true);
        
        /*
         * Set SVG paths to Manual buttons
         */
        
        btnFowr.setId("key-button");
        Label icon = new Label();
        icon.getStyleClass().add("arrowUp");
        btnFowr.setText(null);
        btnFowr.setGraphic(icon);
        btnFowr.setPrefSize(50, 50);
        
        btnBack.setId("key-button");
        icon = new Label();
        icon.getStyleClass().add("arrowDown");
        btnBack.setText(null);
        btnBack.setGraphic(icon);
        btnBack.setPrefSize(50, 50);
        
        btnLeft.setId("key-button");
        icon = new Label();
        icon.getStyleClass().add("arrowLeft");
        btnLeft.setText(null);
        btnLeft.setGraphic(icon);
        btnLeft.setPrefSize(50, 50);
        
        btnRight.setId("key-button");
        icon = new Label();
        icon.getStyleClass().add("arrowRight");
        btnRight.setText(null);
        btnRight.setGraphic(icon);
        btnRight.setPrefSize(50, 50);
        
        btnVelUp.setId("key-button");
        icon = new Label();
        icon.getStyleClass().add("arrowVelUp");
        btnVelUp.setText(null);
        btnVelUp.setGraphic(icon);
        btnVelUp.setPrefSize(50, 50);
        
        btnVelDown.setId("key-button");
        icon = new Label();
        icon.getStyleClass().add("arrowVelDown");
        btnVelDown.setText(null);
        btnVelDown.setGraphic(icon);
        btnVelDown.setPrefSize(50, 50);
        
        /*
         * Initialize GPIO
         */
        GPIO=new ControlGpio();
        
        /*
         * Initialize Serial Port, with the XBee #1 connected on the USB port
         */
        serial=new Serial();
        try {
            System.out.println("Connecting to serial port...");
            serial.connect( "/dev/ttyUSB0" );
        } catch( Exception e ) {
            System.out.println("Error connecting to serial port: "+e.getMessage());
        }
        
        /*
         * Service to start reading serial port for NXT Status
         * It will stop and close when requested
         */
        serviceSerial=new Service<Void>(){

            @Override
            protected Task<Void> createTask() {
                
                return new Task<Void>(){

                    @Override
                    protected Void call() throws Exception {
                        System.out.println("start reading...");
                        serial.read();
                        return null;
                    }    
                    @Override protected void cancelled() {
                        System.out.println("cancelling...");
                        serial.disconnectInput();
                        super.cancelled();
                    }
                };
            }
        };
                
        /*
         * iStatus: [0-3], bound to Serial readings
         */
        iStatus=new SimpleIntegerProperty(0);
        iStatus.bind(Serial.getIntStatus());
        iStatus.addListener(new ChangeListener<Number>(){

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, final Number t1) {
                
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        
                        if(t1!=null && t1.intValue()>=0){
                            /*
                             * Control if NXT responds to commands sent from app
                             * Accordingly the received response:
                             * -the main buttons are enabled/disabled 
                             * -GPIO Alarm if State==Wall Detected
                             * -Label lblStatus text is updated with status
                             * -Manual panel is enabled/disabled
                             */
                            System.out.println("NXT responded: "+t1.intValue());
                            switch(t1.intValue()){
                                case ACK_FORWARD:
                                    btnStart.setDisable(true); btnStop.setDisable(false);
                                    btnQuit.setDisable(true); btnManual.setDisable(false);
                                    panManual.setDisable(true); btnStop.setText("Stop");
                                    GPIO.setAlarmOff();
                                    lblStatus.setText("Drive Forward");
                                    break;
                                case ACK_WALL:
                                    btnStart.setDisable(true); btnStop.setDisable(false);
                                    btnQuit.setDisable(true); btnManual.setDisable(false);
                                    panManual.setDisable(true); btnStop.setText("Stop");
                                    GPIO.setAlarmOn();
                                    lblStatus.setText("Wall Detected");
                                    break;
                                case ACK_STOP: 
                                    btnStart.setDisable(false);  btnQuit.setDisable(false);
                                    btnStop.setDisable(panManual.isDisabled());
                                    btnManual.setDisable(panManual.isDisabled());
                                    panManual.setDisable(true);  btnStop.setText("Stop");
                                    GPIO.setAlarmOff();
                                    lblStatus.setText("Stopped");
                                    break;
                                case ACK_MANUAL:
                                    btnStart.setDisable(true); btnStop.setDisable(false);
                                    btnManual.setDisable(true); btnQuit.setDisable(true);
                                    panManual.setDisable(false); btnStop.setText("Auto");
                                    GPIO.setAlarmOff();
                                    lblStatus.setText("Manual Mode");
                                    break;
                            }
                        }
                    }
                });
            }
        });
        
        serviceSerial.start();
        
    }    
    
    /*
     * Main buttons actions
     */
    
    @FXML
    private void handleStartAction(ActionEvent event) {
        System.out.println("Sending START to serial port...");
        serial.write(START);
    }
    @FXML
    private void handleStopAction(ActionEvent event) {
        if(btnStop.getText().equals("Auto")){
            System.out.println("Sending AUTO to serial port...");
            serial.write(AUTO);
        } else {
            System.out.println("Sending STOP to serial port...");
            serial.write(STOP);
        }
    }
    @FXML
    private void handleManualAction(ActionEvent event) {
        System.out.println("Sending MANUAL to serial port...");
        serial.write(MANUAL);
    }

    @FXML
    private void handleQuitAction(ActionEvent event) {
        System.out.println("Sending QUIT to serial port...");
        serial.write(QUIT);
        GPIO.setLedsOff();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                btnStart.setDisable(false);
                btnStop.setDisable(true);
                btnManual.setDisable(true);
                btnQuit.setDisable(true);
            }
        });
    }
    
    /*
     * MANUAL Panel Actions
     */
    
    @FXML
    private void handleFowardAction(ActionEvent event) {
        System.out.println("Sending FORWARD to serial port...");
        serial.write(FORWARD);
    }
    @FXML
    private void handleBackwardAction(ActionEvent event) {
        System.out.println("Sending BACKWARD to serial port...");
        serial.write(BACKWARD);
    }
    @FXML
    private void handleLeftAction(ActionEvent event) {
        System.out.println("Sending LEFT to serial port...");
        serial.write(LEFT);
    }
    @FXML
    private void handleRightAction(ActionEvent event) {
        System.out.println("Sending RIGHT to serial port...");
        serial.write(RIGHT);
    }
    @FXML
    private void handleVelUpAction(ActionEvent event) {
        System.out.println("Sending SPEED UP to serial port...");
        serial.write(SPEED_UP);
    }
    @FXML
    private void handleVelDownAction(ActionEvent event) {
        System.out.println("Sending SPEED DOWN to serial port...");
        serial.write(SPEED_DW);
    }

    /*
     * EXIT Action. All threads MUST be stopped before exiting
     */

    @FXML
    private void handleExitAction(ActionEvent event) {
        System.out.println("Handle Exit...");
        GPIO.setLedsOff();
        
        System.out.println("Disconnecting service...");
        serviceSerial.cancel();
        
        final Service service=new Service<Void>(){

            @Override
            protected Task<Void> createTask() {
                
                return new Task<Void>(){

                    @Override
                    protected Void call() throws Exception {
                        System.out.println("Disconnecting serial...");
                        serial.disconnect();
                        return null;
                    }                    
                };
            }
        };
        service.stateProperty().addListener(new ChangeListener<Worker.State>(){

            @Override
            public void changed(ObservableValue<? extends Worker.State> ov, Worker.State t, Worker.State t1) {
                if(service.getException()!=null){
                    System.out.println("Exception: "+service.getException().getMessage()); 
                    Platform.exit();
                } else if(t1==Worker.State.SUCCEEDED ||
                          t1==Worker.State.CANCELLED ||
                          t1==Worker.State.FAILED) {
                    System.out.println("End service with: "+t1.toString());
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("Exiting JavaFX app...");
                            Platform.exit();
                        }
                    });
                }                
            }
        });
        
        service.restart();
    }
}
