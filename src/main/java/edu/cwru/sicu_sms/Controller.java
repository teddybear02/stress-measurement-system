/*                                                                      *\
**                    SICU Stress Measurement System                    **
**                      Project P04  |  C380 Team A                     **
**          EBME 380: Biomedical Engineering Design Experience          **
**                    Case Western Reserve University                   **
**                          2016 Fall Semester                          **
\*                                                                      */

package edu.cwru.sicu_sms;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * The controller for the front-end program.
 *
 * @since October 13, 2016
 * @author Ted Frohlich <ttf10@case.edu>
 * @author Abby Walker <amw138@case.edu>
 */
public class Controller {
    
    @FXML private Menu connectMenu;
    @FXML public ToggleGroup connectGroup;
    @FXML private ToggleButton recordButton;
    
    private FileWriter fileWriter;
    private SerialPort serialPort;
    private ObservableList<String> serialPortList;
    
    /**
     * Construct a controller for the front-end program by performing the setup routine:
     * <ul>
     *     1. Detect serial ports. <br>
     *     2. Connect to the first port by default, if it exists. <br>
     * </ul>
     */
    public Controller() {
        boolean portDetected = detectSerialPorts();
        if (portDetected) {
            // TODO: Connect to first port by default.
        }
    }
    
    @FXML
    private void confirmExit() throws Exception {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Exit");
        alert.setHeaderText("Are you sure you want to exit?");
        
        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (result == ButtonType.OK) {
            disconnect();
            Platform.exit();
        }
    }
    
    @FXML
    public void connect(ActionEvent actionEvent) {
        String portName = "COM5";//connectGroup.getSelectedToggle().toString();  // TODO: Doesn't work.
        try {
            System.out.println("Connecting to serial port " + portName + "...");
            if (serialPort != null && serialPort.isOpened()) {
                System.out.println("\t->\tAlready connected!");
            } else {
                serialPort = new SerialPort(portName);
                serialPort.openPort();
                System.out.println("\t->\tSuccessfully connected!");
            }
        } catch (SerialPortException e) {
            System.out.println("\t->\tCouldn't connect!");
        }
    }
    
    @FXML
    private void onMouseEnteredRecordButton() {
        recordButton.setText((isRecording() ? "Stop" : "Start") + " Recording");
    }
    
    @FXML
    private void onMouseExitedRecordButton() {
        recordButton.setText("Record" + (isRecording() ? "ing..." : ""));
    }
    
    @FXML
    private void onMousePressedRecordButton() {
        recordButton.setStyle("-fx-background-color: darkred");
    }
    
    @FXML
    private void onMouseReleasedRecordButton() {
        recordButton.setStyle("-fx-background-color: red");
    }
    
    @FXML
    public void onShowingConnectMenu(Event event) {
        connectMenu.getItems().clear();
        String[] portNames = SerialPortList.getPortNames();
        if (portNames.length == 0) {
            MenuItem dummy = new MenuItem("<no ports available>");
            dummy.setDisable(true);
            connectMenu.getItems().add(dummy);
            return;
        }
        for (String portName : portNames) {
            connectMenu.getItems().add(new RadioMenuItem(portName));
        }
    }
    
    @FXML
    private void record() {
        if (isRecording()) {  // start recording...
            //TODO: Run thread for saving data to file.
        }
        else {  // stop recording...
            //TODO: End thread for saving data to file.
        }
        onMouseEnteredRecordButton();  // indicate what next click would do
    }
    
    /**
     * Initialize the list of detected ports.
     *
     * @return <code>true</code> if at least one serial port was detected; <code>false</code> otherwise
     */
    private boolean detectSerialPorts() {
        serialPortList = FXCollections.observableArrayList();
        serialPortList.addAll(SerialPortList.getPortNames());
        return !serialPortList.isEmpty();
    }
    
    /**
     * Disconnect from the serial port.
     *
     * @return <code>true</code> if the serial port was successfully disconnected; <code>false</code> if something went wrong
     */
    private boolean disconnect() {
        boolean success = false;
        try {
            System.out.println("Disconnecting from serial port " + serialPort.getPortName() + "...");
            success = serialPort.closePort();
            if (success){
                System.out.println("\t->\tSuccessfully disconnected!");
            } else {
                throw new SerialPortException(serialPort.getPortName(),
                        "disconnect()", "Couldn't disconnect!");
            }
        } catch (SerialPortException e) {
            System.out.println("\t->\tAlready disconnected.");
        }
        return success;
    }
    
    private boolean isRecording() {
        return recordButton.isSelected();
    }
    
    
    /**
     * A controller for the EEG tab.
     */
    private class EEGController {
    
        @FXML private LineChart<String, Number>
                leftRostralChart, rightRostralChart,
                leftCaudalChart,  rightCaudalChart;
    
        private CategoryAxis xAxis;
        private NumberAxis yAxis;
        private ObservableList<String> xAxisCategories;
        
        private LineChart.Series<String, Number>[] electrodes;
        
        private ObservableList<LineChart.Data<String, Number>>
                leftRostralList, rightRostralList,
                leftCaudalList,  rightCaudalList;
    
        private int lastObservedChangelistSize, changesBeforeUpdate = 10;
        private Task<Date> chartUpdateTask;
    
        private EEGController() {
            initObservableLists();
            getObservableLists().forEach(list ->
                    list.addListener(dataListChangeListener()));
            
            initAxes();
            xAxis.setCategories(xAxisCategories);
//            xAxis.setAutoRanging(false);
            
            //TODO: instantiate and add data series
            
            initChartUpdateTask();
            Executors.newSingleThreadExecutor().submit(chartUpdateTask);
        }
        
        private void initAxes() {
            xAxis = new CategoryAxis();     yAxis = new NumberAxis();
            xAxis.setLabel("Time (sec)");   yAxis.setLabel("Relative Amplitude");
        }
        
        private void initChartUpdateTask() {
            chartUpdateTask = new Task<Date>() {
                @Override
                protected Date call() throws Exception {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        if (isCancelled()) break;
                        updateValue(new Date());
                    }
                    return new Date();
                }
            };
            chartUpdateTask.valueProperty().addListener(new ChangeListener<Date>() {
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");  //TODO: eventually just want seconds
                Random random = new Random();
                @Override
                public void changed(ObservableValue<? extends Date> observableDate, Date oldDate, Date newDate) {
                    String strDate = dateFormat.format(newDate);
                    xAxisCategories.add(strDate);
                    getObservableLists().forEach(list ->
                            list.add(new LineChart.Data(strDate, newDate.getMinutes() + random.nextInt(100500))));
                }
            });
        }
        
        private void initObservableLists() {
            leftRostralList = rightRostralList = leftCaudalList = rightCaudalList
                    = FXCollections.observableArrayList();
            xAxisCategories
                    = FXCollections.observableArrayList();
        }
        
        private List<LineChart<String, Number>> getCharts() {
            List<LineChart<String, Number>> charts = Collections.emptyList();
            charts.add(leftRostralChart);   charts.add(rightRostralChart);
            charts.add(leftCaudalChart);    charts.add(rightCaudalChart);
            return charts;
        }
    
        private List<ObservableList<LineChart.Data<String, Number>>> getObservableLists() {
            List<ObservableList<LineChart.Data<String, Number>>> lists = Collections.emptyList();
            lists.add(leftRostralList);   lists.add(rightRostralList);
            lists.add(leftCaudalList);    lists.add(rightCaudalList);
            return lists;
        }
        
        private ListChangeListener<LineChart.Data<String, Number>> dataListChangeListener() {
            return change -> {
                if (change.getList().size() - lastObservedChangelistSize > changesBeforeUpdate) {
                    lastObservedChangelistSize += changesBeforeUpdate;
                    xAxis.getCategories().remove(0, changesBeforeUpdate);
                }
            };
        }
        
    }
    
    
    /**
     * A controller for the EKG tab.
     */
    private class EKGController {
        
        //TODO:
        
    }
    
}
