package adbm.antidote.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class AntidoteController implements PropertyChangeListener
{
    public static final String StartDC = "StartDC";
    public static final String StopDC = "StopDC";
    public static final String AddDC = "AddDC";
    public static final String RemoveDC = "RemoveDC";

    public static final String AddKey = "AddKey";
    public static final String RemoveKey = "RemoveKey";
    public static final String ExecuteKeyOperation = "ExecuteKeyOperation";

    public static final String ResetDCConnection = "ResetDCConnection";

    public static final String AddDCConnection = "AddDCConnection";
    public static final String RemoveDCConnection = "RemoveDCConnection";
    public static final String SuspendDCConnection = "SuspendDCConnection";


    public static final String DCListChanged = "RunningDCListChanged";

    public static final String KeyValueChanged = "KeyValueChanged";
    public static final String KeyListChanged = "KeyListChanged";

    public static final String DCConnectionListChanged = "DCConnectionListChanged";


    public void StartDCEvent(String name)
    {
        setModelProperty(StartDC, name);
    }

    public void StopDCEvent(String name)
    {
        setModelProperty(StopDC, name);
    }

    public void AddDCEvent(String name)
    {
        setModelProperty(AddDC, name);
    }

    public void RemoveDCEvent(String name)
    {
        setModelProperty(RemoveDC, name);
    }

    public void AddKeyEvent(String name, String type)
    {
        setModelProperty(AddKey, name, type);
    }

    //A key can only have a single type
    public void RemoveKeyEvent(String name)
    {
        setModelProperty(RemoveKey, name);
    }

    public void ExecuteKeyOperationEvent(String name, String operation, String value)
    {
        setModelProperty(ExecuteKeyOperation, name, operation, value);
    }

    public void ResetDCConnectionEvent(String name)
    {
        setModelProperty(ResetDCConnection, name);
    }

    public void AddDCConnectionEvent(String dc1name, String dc2name)
    {
        setModelProperty(AddDCConnection, dc1name, dc2name);
    }

    public void RemoveDCConnectionEvent(String dc1name, String dc2name)
    {
        setModelProperty(RemoveDCConnection, dc1name, dc2name);
    }

    public void SuspendDCConnectionEvent(String dc1name, String dc2name)
    {
        setModelProperty(SuspendDCConnection, dc1name, dc2name);
    }




    private ArrayList<AntidoteView> registeredViews;
    private ArrayList<AntidoteModel> registeredModels;

    public AntidoteController() {
        registeredViews = new ArrayList<AntidoteView>();
        registeredModels = new ArrayList<AntidoteModel>();
    }


    public void addModel(AntidoteModel model) {
        registeredModels.add(model);
        model.addPropertyChangeListener(this);
    }

    public void removeModel(AntidoteModel model) {
        registeredModels.remove(model);
        model.removePropertyChangeListener(this);
    }

    public void addView(AntidoteView view) {
        registeredViews.add(view);
    }

    public void removeView(AntidoteView view) {
        registeredViews.remove(view);
    }


    //  Use this to observe property changes from registered models
    //  and propagate them on to all the views.


    public void propertyChange(PropertyChangeEvent evt) {

        for (AntidoteView view: registeredViews) {
            view.modelPropertyChange(evt);
        }
    }


    /**
     * This is a convenience method that subclasses can call upon
     * to fire property changes back to the models. This method
     * uses reflection to inspect each of the model classes
     * to determine whether it is the owner of the property
     * in question. If it isn't, a NoSuchMethodException is thrown,
     * which the method ignores.
     *
     * @param propertyName = The name of the property.
     * @param newValue1 = An object that represents the new value
     * of the property.
     */
    protected void setModelProperty(String propertyName, Object newValue1) {

        for (AntidoteModel model : registeredModels) {
            try {

                Method method = model.getClass().
                        getMethod(propertyName,


                                  newValue1.getClass());
                method.invoke(model, newValue1);

            } catch (Exception ex) {
                //  Handle exception.
            }
        }
    }

    protected void setModelProperty(String propertyName, Object newValue1, Object newValue2) {

        for (AntidoteModel model : registeredModels) {
            try {

                Method method = model.getClass().getMethod(propertyName, newValue1.getClass(), newValue2.getClass());
                method.invoke(model, newValue1, newValue2);

            } catch (Exception ex) {
                //  Handle exception.
            }
        }
    }

    protected void setModelProperty(String propertyName, Object newValue1, Object newValue2, Object newValue3) {

        for (AntidoteModel model : registeredModels) {
            try {

                Method method = model.getClass().getMethod(propertyName, newValue1.getClass(), newValue2.getClass(), newValue3.getClass());
                method.invoke(model, newValue1, newValue2, newValue3);

            } catch (Exception ex) {
                //  Handle exception.
            }
        }
    }
}
