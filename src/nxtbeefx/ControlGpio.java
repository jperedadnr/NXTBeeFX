package nxtbeefx;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

/**
 * Control of a GPIO pins on the Raspberry Pi to show
 * alarm (red LED on) in case NXT finds an obstacle, 
 * no alarm (green LED on) otherwise while app is on
 * 
 * @author José Pereda Llamas
 * Created on 27-dic-2012 - 19:32:10
 */
public class ControlGpio
{
    // create gpio controller
    final GpioController gpio = GpioFactory.getInstance();

    // provision gpio pin #01 as an output pin
    final GpioPinDigitalOutput pinRed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "MyLEDRed", PinState.LOW);

    // provision gpio pin #07 as an output pin
    final GpioPinDigitalOutput pinGreen = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07, "MyLEDGreen", PinState.LOW);
    
    public ControlGpio()
    {
        System.out.println("<--Pi4J--> GPIO Control Example ... started.");
        // turn off gpio pin #01
        pinRed.low();
        pinGreen.low();
    }
    
    public void setAlarmOn() {
        pinRed.high();
        pinGreen.low();
    }
    
    public void setAlarmOff() {
        pinRed.low();
        pinGreen.high();
    }

    public void setLedsOff()  {
        pinRed.low();
        pinGreen.low();
    }
}