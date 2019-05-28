
package com.PIDController;
/* 
 * Author: Jessica Van Brummelen
 * Email: jess.vanbrummelen AT gmail.com
 */

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.*;

import android.util.Log;

@DesignerComponent(version = PIDController.VERSION, description = "An extension to control a system via a simple PID controller."
		+ "This controller generates an output signal based on the setpoint, the measured signal, and controller gains.", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "images/extension.png")
@SimpleObject(external = true)
public class PIDController extends AndroidNonvisibleComponent implements Component {

	public static final int VERSION = 1;
	public static final float DEFAULT_KP = 0f;
	public static final float DEFAULT_KI = 0f;
	public static final float DEFAULT_KD = 0f;
	public static final float DEFAULT_SETPOINT = 0f;
	public static final float DEFAULT_OUTPUTMAX = 0f;
	public static final float DEFAULT_OUTPUTMIN = 0f;
	
	private ComponentContainer container;
	// Whether the controller is completing the first iteration:
	private boolean FirstLoop = true;
	// Controller gains (proportional, integral, derivative):
	private double Kp;
	private double Ki;
	private double Kd;
	// Controller target value:
	private double Setpoint;
	// Constraints on the output value:
	private double OutputMax;
	private double OutputMin;
	// Error between the actual and measured values:
	private double Error;
	// Sum of the error to compute integral output value:
	private double ErrorSum;
	// Measured value from previous iteration:
	private double PrevMeasuredVal;

	/**
	 * Creates a new PIDController without constraints on the output
	 * value.
	 * 
	 * @param kp:
	 *            the proportional controller gain
	 * @param ki:
	 *            the integral controller gain
	 * @param kd:
	 *            the derivative controller gain
	 * @param setpoint:
	 *            the desired setpoint for what's being measured
	 */
	public PIDController(ComponentContainer container) {
		super(container.$form());
		this.container = container;

		Kp(DEFAULT_KP);
		Ki(DEFAULT_KI);
		Kd(DEFAULT_KD);
		Setpoint(DEFAULT_SETPOINT);
		OutputMax(DEFAULT_OUTPUTMAX);
		OutputMin(DEFAULT_OUTPUTMIN);
	}
	
	/*
	 * ******** PID Methods (Public) ********
	 */
	/**
	 * Calculates the output from the controller that should be sent to the
	 * system based on the current measured value from the system. This is the
	 * main function of the controller.
	 * 
	 * @param measuredVal
	 * @return output
	 */
	@SimpleFunction(description = "Calculate "
			+ "the output from the controller that should be sent to the "
			+ "system. The output is calculated based on the current measured value "
			+ "from the system, the previous measured value, the setpoint, the sum of "
			+ "the past error, etc.  Calculating this output is the main function of "
			+ "the controller.")
	public double calcOutput(double measuredVal) {
		// Calculate the difference between the setpoint and the measured value
		Error = calcError(Setpoint, measuredVal);

		/*
		 * If the controller has just started or been reset, ensure that the
		 * prevMeasuredVal and prevOutput variables have reasonable values.
		 */
		if (FirstLoop) {
			PrevMeasuredVal = measuredVal;
			// prevOutput = kp * error;
			FirstLoop = false;
		}

		// Calculate the p, i, and d terms and add them to get the output
		double proportionalVal = calcProportionalVal(Error, Kp);
		double integralVal = calcIntegralVal(ErrorSum, Ki);
		double derivativeVal = calcDerivativeVal(measuredVal, PrevMeasuredVal, Kd);

		// Get the output by summing
		double output = proportionalVal + integralVal + derivativeVal;

		/*
		 * If the output is constrained (i.e., maxOutput != minOutput), then
		 * constrain the output.
		 */
		if (isOutputConstrained()) {
			/*
			 * Ensure output isn't larger than the max value allowed or smaller
			 * than the min value allowed.
			 */
			if (output > OutputMax) {
				output = OutputMax;
			}
			if (output < OutputMin) {
				output = OutputMin;
			}
		}

		/*
		 * If the output is larger than output max or less than outputMin (and
		 * the output is constrained) then reset the errorSum to a reasonable
		 * level.
		 */
		if (isOutputConstrained() &&  (output >= OutputMax || output <= OutputMin)) {
			//!(output > outputMin && output < outputMax)){
			// Reset errorSum to reasonable level (just the error).
			ErrorSum = Error;
		} else {
			ErrorSum += Error;
		}

		// Update the prev. values for next iteration and return the output.
		PrevMeasuredVal = measuredVal;
		// prevOutput = output;
		return output;
	}
	
	/**
	 * Resets the PIDController. This sets the errorSum to zero, the number of
	 * loops to zero (i.e., firstLoop = true).
	 */
	@SimpleFunction(description = "Reset the PID Controller. This sets the error and errorSum "
					+ "to zero, the number of loops to zero (i.e., firstLoop = true).")
	public void resetController() {
		this.Error = 0;
		this.ErrorSum = 0;
		this.FirstLoop = true;
	}
	
	/**
	 * Returns true if the output is constrained by outputMax and outputMin. If
	 * outputMax and outputMin are equal, then it is assumed that the output is
	 * unconstrained (returns false).
	 * 
	 * @return constrained
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Returns true if the "
			+ "output is constrained by outputMax and outputMin. If outputMax and outputMin are "
			+ "equal, then it is assumed that the output is unconstrained (returns false).")
	public boolean isOutputConstrained() {
		boolean constrained = false;
		/*
		 * if the difference between outputMax and outputMin is greater than
		 * 0.01% of outputMax, then outputMax and outputMin are considered
		 * different, and the output is constrained.
		 */
		if (Math.abs(OutputMax - OutputMin) > Math.abs(0.0001 * OutputMax)) {
			constrained = true;
		}
		return constrained;
	}
	
	/*
	 * ******** Helper Methods (Private) ********
	 */
	 
	/**
	 * Tests whether the outputMin value is less than or equal to the outputMax
	 * value. It uses the instance variables outputMin and outputMax from this
	 * object. If outputMin is greater than outputMax, then an error message is
	 * generated.
	 * 
	 * @return correct: if min < max, then correct = true
	 * 
	 * TODO: unclear if this actually works or not... need to test!
	 * --> unsure if the Log.e and/or runtimeexception is correct...
	 */
	private boolean testOutputMinMax() {
		boolean correct = true;
		if (this.OutputMin > this.OutputMax) {
			Log.e("PIDController", "OutputMin is greater than OutputMax. "
					+ "Resetting OutputMin and OutputMax to zero. "
					+ "To avoid this error, if upper bound is greater than zero, set OutputMax first."
					+ "If lower bound is less than zero, set OutputMin first.");
			this.OutputMax = 0;
			this.OutputMin = 0;
			correct = false;
			new RuntimeException().printStackTrace();
		}
		return correct;
	}
	
	/**
	 * Returns the error based on the provided actual value (measuredValue) and
	 * the provided setpoint. Current implementation just subtracts the two
	 * values: setpoint - measuredValue. <br>
	 * Note that this does NOT set the error instance variable.
	 * 
	 * @param measuredValue:
	 *            the actual value (often read by a sensor)
	 * @param setpoint:
	 *            the setpoint determined by the user or external source
	 * @return error
	 */
	private double calcError(double setpoint, double measuredValue) {
		return setpoint - measuredValue;
	}
	
	/**
	 * Returns the "proportional value" based on the current error and provided
	 * proportional gain. This proportional value is summed with the integral
	 * value and derivative value to calculate the output.
	 * 
	 * @param error
	 * @param kp
	 * @return proportional value
	 */
	private double calcProportionalVal(double error, double kp) {
		return kp * error;
	}
	
	/**
	 * Returns the "integral value" based on the current error sum and provided
	 * integral gain. This integral value is summed with the proportional value
	 * and derivative value to calculate the output.
	 * 
	 * @param errorSum:
	 *            sum of the error up until this point.
	 * @param ki:
	 *            integral gain.
	 * @return: integral value
	 */
	private double calcIntegralVal(double errorSum, double ki) {
		return ki * errorSum;
	}
	
	/**
	 * Returns the "derivative value" based on the current measured/actual
	 * value, the previous measured/actual value, and the provided derivative
	 * gain. This derivative value is summed with the proportional value and
	 * integral value to calculate the output.
	 * 
	 * @param measuredValue:
	 *            current actual measured value.
	 * @param prevMeasuredVal:
	 *            previous actual measured value.
	 * @param kd:
	 *            derivative gain.
	 * @return: derivative value
	 */
	private double calcDerivativeVal(double measuredValue, double prevMeasuredValue, double kd) {
		return -1 * kd * (measuredValue - prevMeasuredValue);
	}
	
	/*
	 * ******** Getters and Setters ********
	 */
	// Property Getters
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public double Kp() {
		return Kp;
	}

	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public double Ki() {
		return Ki;
	}

	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public double Kd() {
		return Kd;
	}

	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public double Setpoint() {
		return Setpoint;
	}
	
	@SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get "
			+ "the current error between the measured value and the setpoint.")
	public double Error(){
		return Error;
	}
	
	/**
	 * Gets the maximum value for the output that will be returned by the PID.
	 * <br>
	 * If outputMax and outputMin are equal, then the output is assumed to be
	 * unconstrained.
	 * 
	 * @return outputMax
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public double OutputMax() {
		return OutputMax;
	}
	
	/**
	 * Gets the minimum value for the output that will be returned by the PID.
	 * <br>
	 * If outputMax and outputMin are equal, then the output is assumed to be
	 * unconstrained.
	 * 
	 * @return outputMin
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public double OutputMin() {
		return OutputMin;
	}

	// Property Setters
	
	/**
	 * Sets the proportional gain. Note that if the parameter kp is negative, it
	 * will be reset to a positive value.
	 * 
	 * @param kp
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_FLOAT, defaultValue = PIDController.DEFAULT_KP
			+ "")
	@SimpleProperty(description = "Set the proportional gain, Kp." +
			"Note that if the parameter kp is negative, it will be reset to a positive value.")
	public void Kp(double kp) {
		this.Kp = Math.abs(kp);
	}

	/**
	 * Sets the integral gain. Note that if the parameter ki is negative, it
	 * will be reset to a positive value.
	 * 
	 * @param ki
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_FLOAT, defaultValue = PIDController.DEFAULT_KI
			+ "")
	@SimpleProperty(description = "Set the integral gain, Ki." +
			"Note that if the parameter ki is negative, it will be reset to a positive value.")
	public void Ki(double ki) {
		this.Ki = Math.abs(ki);
	}
	
	/**
	 * Sets the derivative gain. Note that if the parameter kd is negative, it
	 * will be reset to a positive value.
	 * 
	 * @param kd
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_FLOAT, defaultValue = PIDController.DEFAULT_KD
			+ "")
	@SimpleProperty(description = "Set the derivative gain, Kd." +
			"Note that if the parameter kd is negative, it will be reset to a positive value.")
	public void Kd(double kd) {
		this.Kd = Math.abs(kd);
	}
	
	/**
	 * Sets the setpoint (or "target") for the system.
	 * 
	 * @param kd
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = PIDController.DEFAULT_SETPOINT
			+ "")
	@SimpleProperty(description = "Set the setpoint or 'target value' of the system.")
	public void Setpoint(double setpoint) {
		this.Setpoint = setpoint;
	}
	
	/**
	 * Sets the maximum value for the output that will be returned by the PID.
	 * <br>
	 * If outputMax and outputMin are equal, then the output is assumed to be
	 * unconstrained.
	 * 
	 * @param outputMax
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = PIDController.DEFAULT_OUTPUTMAX
			+ "")
	@SimpleProperty(description = "Set the maximum value for the output that will be returned by the PID."
			+ "\n If outputMax and outputMin are equal, then the output is assumed to be unconstrained.")
	public void OutputMax(double outputMax) {
		this.OutputMax = outputMax;
		testOutputMinMax();
	}
	
	/**
	 * Sets the minimum value for the output that will be returned by the PID.
	 * <br>
	 * If outputMax and outputMin are equal, then the output is assumed to be
	 * unconstrained.
	 * 
	 * @param outputMin
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = PIDController.DEFAULT_OUTPUTMIN
			+ "")
	@SimpleProperty(description = "Set the minimum value for the output that will be returned by the PID."
			+ "\n If outputMax and outputMin are equal, then the output is assumed to be unconstrained.")
	public void OutputMin(double outputMin) {
		this.OutputMin = outputMin;
		testOutputMinMax();
	}
}