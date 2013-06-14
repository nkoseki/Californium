package ch.inf.vs.californium.network.layer;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import ch.inf.vs.californium.MessageDeliverer;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.Exchange.Origin;
import ch.inf.vs.californium.network.Matcher;
import ch.inf.vs.californium.network.MessageIntercepter;
import ch.inf.vs.californium.network.NetworkConfig;
import ch.inf.vs.californium.network.RawDataChannel;
import ch.inf.vs.californium.network.connector.Connector;
import ch.inf.vs.californium.network.serializer.DataParser;
import ch.inf.vs.californium.network.serializer.Serializer;
import ch.inf.vs.californium.resources.CalifonriumLogger;

/**
 * The CoAPStack builds up the stack of CoAP layers that process the CoAP
 * protocol.
 * <p>
 * The complete process for incoming and outgoing messages is visualized below.
 * The class <code>CoapStack</code> builds up the part between the Stack Top and
 * Bottom.
 * <hr><blockquote><pre>
 * +-----------------------+
 * | {@link MessageDeliverer}      |
 * +-------------A---------+
 *               A
 *             * A
 * +-----------+-A---------+
 * | {@link Endpoint}  v A         |
 * |           v A         |
 * | +---------v-+-------+ |
 * | | Stack Top         | |
 * | +-------------------+ |
 * | | {@link TokenLayer }        | |
 * | +-------------------+ |
 * | | {@link ObserveLayer}      | |
 * | +-------------------+ |
 * | | {@link BlockwiseLayer}    | |
 * | +-------------------+ |
 * | | {@link ReliabilityLayer}  | |
 * | +-------------------+ |
 * | | Stack Bottom      | |
 * | +---------+-A-------+ |
 * |           v A         |
 * |         {@link Matcher}       |
 * |           v A         |
 * |       {@link MessageIntercepter Intercepter}     |
 * |           v A         |
 * +-----------v-A---------+
 *             v A 
 *             v A 
 * +-----------v-+---------+
 * | {@link Connector}             |
 * +-----------------------+
 * </pre></blockquote><hr>
 */
public class CoapStack {

	/** The LOGGER. */
	final static Logger LOGGER = CalifonriumLogger.getLogger(CoapStack.class);

	/** The list of layers. */
	private List<Layer> layers;
	
	/** The channel. */
	private RawDataChannel channel;

	/** The top of the stack. */
	private StackTopAdapter top;
	
	/** The bottom of the stack. */
	private StackBottomAdapter bottom;

	private MessageDeliverer deliverer;
	
	public CoapStack(NetworkConfig config, RawDataChannel channel) {
		this.top = new StackTopAdapter();
		this.channel = channel;
		this.layers = 
				new Layer.TopDownBuilder()
				.add(top)
				.add(new TokenLayer())
				.add(new ObserveLayer(config))
				.add(new BlockwiseLayer(config))
				.add(new ReliabilityLayer(config))
				.add(bottom = new StackBottomAdapter())
				.create();
	}
	
	// delegate to top
	public void sendRequest(Request request) {
		top.sendRequest(request);
	}

	// delegate to top
	public void sendResponse(Exchange exchange, Response response) {
		top.sendResponse(exchange, response);
	}

	// delegate to top
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		top.sendEmptyMessage(exchange, message);
	}

	// delegate to bottom
	public void receiveRequest(Exchange exchange, Request request) {
		bottom.receiveRequest(exchange, request);
	}

	// delegate to bottom
	public void receiveResponse(Exchange exchange, Response response) {
		bottom.receiveResponse(exchange, response);
	}

	// delegate to bottom
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		bottom.receiveEmptyMessage(exchange, message);
	}

	public void setExecutor(ScheduledExecutorService executor) {
		for (Layer layer:layers)
			layer.setExecutor(executor);
	}
	
	public void setDeliverer(MessageDeliverer deliverer) {
		this.deliverer = deliverer;
	}
	
	private class StackTopAdapter extends AbstractLayer {
		
		public void sendRequest(Request request) {
			Exchange exchange = new Exchange(request, Origin.LOCAL);
			sendRequest(exchange, request); // layer method
		}
		
		@Override
		public void sendRequest(Exchange exchange, Request request) {
			assert(exchange == null);
			exchange.setRequest(request);
			super.sendRequest(exchange, request);
		}
		
		@Override
		public void sendResponse(Exchange exchange, Response response) {
			exchange.setResponse(response);
			super.sendResponse(exchange, response);
		}
		
		@Override
		public void receiveRequest(Exchange exchange, Request request) {
			if (exchange.getRequest() == null)
				throw new NullPointerException("Final assembled request of exchange must not be null");
			if (deliverer != null) {
				deliverer.deliverRequest(exchange);
			} else {
				LOGGER.severe("Top of CoAP stack has no deliverer to deliver request");
			}
		}

		@Override
		public void receiveResponse(Exchange exchange, Response response) {
			if (deliverer != null) {
				LOGGER.info("Top of CoAP stack delivers response");
				deliverer.deliverResponse(exchange, response); // notify request that response has arrived
			} else {
				LOGGER.severe("Top of CoAP stack has no deliverer to deliver response");
			}
		}
		
		@Override
		public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
			ignore(message);
		}
	}
	
	private class StackBottomAdapter extends AbstractLayer {
	
		@Override
		public void sendRequest(Exchange exchange, Request request) {
			channel.sendRequest(exchange, request);
		}

		@Override
		public void sendResponse(Exchange exchange, Response response) {
			channel.sendResponse(exchange, response);
		}

		@Override
		public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
			channel.sendEmptyMessage(exchange, message);
		}
		
	}
}