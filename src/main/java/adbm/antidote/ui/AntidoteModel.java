package adbm.antidote.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 */
public class AntidoteModel
{

    private static final Logger log = LogManager.getLogger(AntidoteModel.class);

    protected PropertyChangeSupport propertyChangeSupport;

    /**
     *
     */
    public AntidoteModel()
    {
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     *
     * @param propertyName
     * @param oldValue
     * @param newValue
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }


}
