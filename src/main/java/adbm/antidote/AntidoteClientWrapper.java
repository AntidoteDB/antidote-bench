package adbm.antidote;

import adbm.antidote.ui.AntidoteController;
import adbm.antidote.ui.AntidoteModel;
import adbm.docker.DockerManager;
import adbm.settings.MapDBManager;
import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import static adbm.settings.MapDBManager.getTypeOfKey;
import static eu.antidotedb.client.Key.create;

public class AntidoteClientWrapper extends AntidoteModel
{

    private final AntidoteClient antidote;

    private final Bucket bucket;

    public final int hostPort;

    public final String name;

    private boolean running;

    public AntidoteClientWrapper(String name)
    {
        //docker run -i -t -d --name antidote1 -p 8087:8087 --network antidote_ntwk -e SHORT_NAME=true -e NODE_NAME=antidote@antidote1 antidotedb/antidote
        hostPort = DockerManager.getHostPortFromContainer(name);

        antidote = new AntidoteClient(new InetSocketAddress("localhost", hostPort));

        bucket = Bucket.bucket(name + "bucket");

        this.name = name;

        running = true;
    }

    public void stop()
    {
        if (running) {
            this.firePropertyChange(AntidoteController.DCListChanged, "", "");
            running = false;
        }
    }

    public void start()
    {
        if (!running) {
            this.firePropertyChange(AntidoteController.DCListChanged, "", "");
            running = false;
        }
    }

    public void AddKey(String name, AntidotePB.CRDT_type type)
    {
        if (running) {
            AntidoteStaticTransaction tx = antidote.createStaticTransaction();
            bucket.update(tx, create(type, ByteString.copyFromUtf8(name)).reset());
            tx.commitTransaction();
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
    }

    public void RemoveKey(String name){
        if (running) {
            MapDBManager.removeKey(name);
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
    }

    public void ExecuteKeyOperation(String name, String operation, Object value)
    {
        if (running) {
            Key key = create(getTypeOfKey(name), ByteString.copyFromUtf8(name));
            UpdateOp update = null;
            if (!operation.equals("reset")) {
                if (operation.equals("increment") || operation.equals("decrement") || operation.equals("set")) {
                    value = Long.parseLong(value.toString());
                    long lvalue = (long) value;
                    if (operation.equals("decrement")) {
                        operation = "increment";
                        lvalue = -lvalue;
                    }
                    if (operation.equals("set")) {
                        operation = "assign";
                    }
                    Class[] par = new Class[1];
                    par[0] = long.class;
                    try {
                        Method method = key.getClass().getMethod(operation, par);
                        update = (UpdateOp) method.invoke(key, lvalue);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    if (operation.equals("assign")) {
                        Class[] par = new Class[1];
                        par[0] = Object.class;
                        try {
                            Method method = key.getClass().getMethod(operation, par);
                            update = (UpdateOp) method.invoke(key, value);
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            else {
                try {
                    Method method = key.getClass().getMethod(operation, null);
                    update = (UpdateOp) method.invoke(key, null);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            if (update != null) {
                AntidoteStaticTransaction tx = antidote.createStaticTransaction();
                bucket.update(tx, update);
                tx.commitTransaction();
            }
            this.firePropertyChange(AntidoteController.KeyValueChanged, "", "");
        }
    }

    // read
    public String getKeyValue(String name)
    {
        if (running) {
            return bucket.read(antidote.noTransaction(), createKey(name)).toString();
        }
        return null;
    }

    private Key createKey(String name)
    {
        if (running) {
            return create(getTypeOfKey(name), ByteString.copyFromUtf8(name));
        }
        return null;
    }

}
