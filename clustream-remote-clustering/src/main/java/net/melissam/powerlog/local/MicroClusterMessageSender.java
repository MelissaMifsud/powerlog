package net.melissam.powerlog.local;

import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQObjectMessage;

import net.melissam.powerlog.clustering.*;
import net.melissam.powerlog.messaging.MicroClusterMessage;


/**
 * JMS client for sending snapshots to the cental node.
 * @author melissam
 *
 */
public class MicroClusterMessageSender {

	//the id of the instance this sender is sending on behalf of
	private int instanceId;
	
	// connection to message queue
    private Connection connection;
    
    // session for sending messages
    private Session session;
    
    // message producer
    private MessageProducer messageProducer;
    
    
    public MicroClusterMessageSender(int instanceId, String host, int port, String queueName) throws JMSException{
    	
    	this.instanceId = instanceId;

        // create a Connection Factory
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://" + host + ":" + port);

        // create a Connection
        connection = connectionFactory.createConnection();
        connection.setClientID(String.valueOf("LocalClusterer-" + instanceId));

        // create a Session
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // create the Queue to send messages to
        Queue queue = session.createQueue(queueName);

        // create a MessageProducer for sending messages
        messageProducer = session.createProducer(queue);
    	
    }
    
    public void send(List<MicroCluster> microClusters, int timestamp) throws JMSException{
    	
    	//for (MicroCluster cluster : microClusters){

    		MicroClusterMessage message = new MicroClusterMessage(instanceId, timestamp);
    		message.setMicroClusters(microClusters);

    		ObjectMessage msg = new ActiveMQObjectMessage();
    		msg.setObject(message);

    		messageProducer.send(msg);
    		
    		try{
    			Thread.sleep(25);
    		}catch(InterruptedException ex){}

    	//}
    	
    }
    
    public void stop() throws JMSException{    	
    	messageProducer.close();
    	connection.close();
    }
	
}
