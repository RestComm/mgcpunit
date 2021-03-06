package org.mobicents.commtesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.mobicents.arquillian.mediaserver.api.MgcpEventListener;
import org.mobicents.arquillian.mediaserver.api.MgcpUnitRequest;
import org.mobicents.commtesting.mgcpUnit.requests.NotifyRequest;
import org.mobicents.commtesting.mgcpUnit.requests.PlayAnnouncementRequest;
import org.mobicents.commtesting.mgcpUnit.utils.EventTypes;
import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.message.MgcpMessage;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.MgcpResponse;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.spi.listener.Event;
import org.mobicents.media.server.utils.Text;

/**
 * Implementation of the MgcpEventListener interface.
 * 
 * Register an instance of this class with an embedded Mediaserver in order to receive all the Mgcp requests and resources. 
 * The class classifies the Mgcp events according to the event id and provides helpful methods to manage them. 
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas@gmail.com</a>
 */

public class MgcpEventListenerImpl implements MgcpEventListener {

	private Logger logger = Logger.getLogger(MgcpEventListenerImpl.class);

	private ArrayList<MgcpRequest> requests = new ArrayList<MgcpRequest>();
	private ArrayList<MgcpResponse> responses = new ArrayList<MgcpResponse>();
	private ArrayList<MgcpUnitRequest> playAnnouncementRequests = new ArrayList<MgcpUnitRequest>();
	private ArrayList<MgcpUnitRequest> notifyRequests = new ArrayList<MgcpUnitRequest>();

	private MgcpRequest lastRequestMessage;
	private MgcpResponse lastResponseMessage;

	
	/**
	 * Process an Mgcp Event.
	 */
	@Override
	public void process(Event event) {
		MgcpEvent mgcpEvent = (MgcpEvent)event;
		MgcpMessage message = mgcpEvent.getMessage();

		if(mgcpEvent.getEventID() == MgcpEvent.REQUEST){
			MgcpRequest req = (MgcpRequest)message;
			requests.add(req);
			setLastRequestMessage(req);
			classifyEvent(mgcpEvent);
		} else {
			MgcpResponse resp = (MgcpResponse)message;
			responses.add(resp);
			setLastResponseMessage(resp);
		}

		logger.debug("New dispatched eventId: "+message.getTxID()+" message: "+mgcpEvent.getMessage());
	}

	/**
	 * Classifies an Mgcp Event as MgcpRequest or MgcpResponse.
	 * 
	 * @param event MgcpEvent
	 */
	private void classifyEvent(MgcpEvent event) {

		MgcpRequest request;
		Text command;

		switch (event.getEventID()) {
		case MgcpEvent.REQUEST :
			request = (MgcpRequest) event.getMessage();
			command = request.getCommand();

			//select action using message type and execute action
			if (command.equals(EventTypes.CREATE_CONNECTION)) {
//				return crcx;
			} else if (command.equals(EventTypes.MODIFY_CONNECTION)) {
//				return mdcx;
			} else if (command.equals(EventTypes.ENDPOINT_CONFIGURATION)) {
//				return epcf;
			} else if (command.equals(EventTypes.DELETE_CONNECTION)) {
				if(request.getParameter(Parameter.REASON_CODE)!=null) {
					//its connection deletion from ms
//					return ntfy;	
				}
				else {
					//its delete connection request
//					return dlcx;
				}
			} else if (command.equals(EventTypes.REQUEST_NOTIFICATION)) {
				Parameter param = request.getParameter(Parameter.REQUESTED_SIGNALS);
				if (param.getValue().toString().contains("AU/pa")) {
					playAnnouncementRequests.add(new PlayAnnouncementRequest(request));
				}
					
			} else if (command.equals(EventTypes.REQUEST_NOTIFY)) {
				notifyRequests.add(new NotifyRequest(request));
			}

			break;
		case MgcpEvent.RESPONSE :
			break;
		}
	}
	/**
	 * Returns all the PlayAnnouncement Mgcp requests
	 * 
	 * @return Collection<MgcpUnitRequest>
	 */
	@Override
	public Collection<MgcpUnitRequest> getPlayAnnoRequestsReceived(){
		if (!playAnnouncementRequests.isEmpty()){
			return playAnnouncementRequests;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns all the Notify Mgcp requests
	 * 
	 * @return Collection<MgcpUnitRequest>
	 */
	@Override 
	public Collection<MgcpUnitRequest> getNotifyRequestsReceived(){
		if (!notifyRequests.isEmpty()){
			return notifyRequests;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns true if for a given TransactionId there was a successful response.
	 * 
	 * @param int TransactionId
	 * @return boolean
	 */
	@Override
	public boolean checkForSuccessfulResponse(int txId){

		boolean result = false;
		
		for (Iterator<MgcpResponse> MgcpResponsesIter = responses.iterator(); MgcpResponsesIter.hasNext();) {
			MgcpResponse resp = MgcpResponsesIter.next();
			if(resp.getTxID()==txId && resp.getResponseCode()==200)
				result = true;
		}
		return result;
	}

	/**
	 * Returns true if for all the Mgcp requests there was a successful response.
	 * 
	 * @return boolean
	 */
	@Override
	public boolean verifyAll(){
		boolean result = false;
		for (MgcpRequest mgcpRequest : requests) {
			result = checkForSuccessfulResponse(mgcpRequest.getTxID());
		}
		return result;
	}
	
	/**
	 * Returns true if for all the Notify Mgcp request there was a successful response
	 * 
	 * @return boolean
	 */
	@Override
	public boolean verifyNotify(){
		boolean result = false;
		if (playAnnouncementRequests.size() == notifyRequests.size())
			result = true;
		
		return result;
	}
	
	/**
	 * Clears the internal storage for Mgcp requests
	 */
	@Override
	public void clearRequests(){
		requests.clear();
	}

	/**
	 * Clears the internal storage for Mgcp responses
	 */
	@Override
	public void clearResponses(){
		responses.clear();
	}
	
	/**
	 * Clears the internal storage for all Mgcp request and responses
	 */
	@Override
	public void clearAll(){
		clearRequests();
		clearResponses();
	}
	
	/**
	 * Returns a list of all Mgcp requests
	 * 
	 * @return ArrayList<MgcpRequest>
	 */
	public ArrayList<MgcpRequest> getRequests() {
		return requests;
	}

	public void setRequests(ArrayList<MgcpRequest> requests) {
		this.requests = requests;
	}


	public ArrayList<MgcpResponse> getResponses() {
		return responses;
	}


	public void setResponses(ArrayList<MgcpResponse> responses) {
		this.responses = responses;
	}

	/**
	 * Get the last received Mgcp request
	 * 
	 * @return MgcpRequest
	 */
	public MgcpRequest getLastRequestMessage() {
		return lastRequestMessage;
	}

	public void setLastRequestMessage(MgcpRequest lastRequestMessage) {
		this.lastRequestMessage = lastRequestMessage;
	}

	/**
	 * Get the last received Mgcp response
	 * 
	 * @return MgcpResponse
	 */
	public MgcpResponse getLastResponseMessage() {
		return lastResponseMessage;
	}


	public void setLastResponseMessage(MgcpResponse lastResponseMessage) {
		this.lastResponseMessage = lastResponseMessage;
	}

}
