package com.eroelf.tfserver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.eroelf.javaxsx.util.ArrayUtil;
import com.eroelf.javaxsx.util.Strings;
import com.eroelf.tfserver.data.ArrayWrapper4J;
import com.eroelf.tfserver.data.ArrayWrapper4Pb;
import com.eroelf.tfserver.data.Sample4J;
import com.eroelf.tfserver.datastream.BoolArray;
import com.eroelf.tfserver.datastream.DataArray;
import com.eroelf.tfserver.datastream.DoubleArray;
import com.eroelf.tfserver.datastream.FloatArray;
import com.eroelf.tfserver.datastream.Int32Array;
import com.eroelf.tfserver.datastream.Int64Array;
import com.eroelf.tfserver.datastream.Param;
import com.eroelf.tfserver.datastream.RequestInfo;
import com.eroelf.tfserver.datastream.Response;
import com.eroelf.tfserver.datastream.Sample;
import com.eroelf.tfserver.datastream.Status;
import com.eroelf.tfserver.datastream.StringArray;
import com.eroelf.tfserver.datastream.TfServiceGrpc;
import com.eroelf.tfserver.datastream.UInt8Array;
import com.eroelf.tfserver.datastream.Sample.Builder;
import com.eroelf.tfserver.datastream.TfServiceGrpc.TfServiceBlockingStub;
import com.eroelf.tfserver.datastream.TfServiceGrpc.TfServiceStub;
import com.eroelf.tfserver.exception.ResponseException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

/**
 * A client for TensorFlow GRPC servers. Provides APIs to control the client as well as to deal with the requesting data flow.
 * 
 * @author weikun.zhong
 */
public class TfGrpcClient
{
	private final ManagedChannel channel;
	private final TfServiceStub asyncStub;
	private final TfServiceBlockingStub blockingStub;

	public static Sample createSample(Sample4J sample)
	{
		return createSample(sample.modelName, sample.modelVersion, sample.signatureName, sample.inputs);
	}

	@SuppressWarnings("unchecked")
	public static Sample createSample(String modelName, String modelVersion, String signatureName, Map<String, ArrayWrapper4J<?>> inputs)
	{
		Builder builder=Sample.newBuilder().setModelName(modelName).setSignatureName(signatureName);
		if(Strings.isValid(modelVersion))
			builder.setModelVersion(modelVersion);
		for(Entry<String, ArrayWrapper4J<?>> entry : inputs.entrySet())
		{
			ArrayWrapper4J<?> arrayWrapper=entry.getValue();
			com.eroelf.tfserver.datastream.DataArray.Builder dataBuilder=DataArray.newBuilder();
			dataBuilder.setTypeNum(arrayWrapper.getType().getNumber());
			for(int v : arrayWrapper.getShape())
			{
				dataBuilder.addShape(v);
			}
			switch(arrayWrapper.getType())
			{
			case DT_FLOAT:
				dataBuilder.setData(Any.pack(FloatArray.newBuilder().addAllData((List<Float>)arrayWrapper.getWrappedData()).build()));
				break;
			case DT_DOUBLE:
				dataBuilder.setData(Any.pack(DoubleArray.newBuilder().addAllData((List<Double>)arrayWrapper.getWrappedData()).build()));
				break;
			case DT_INT32:
				dataBuilder.setData(Any.pack(Int32Array.newBuilder().addAllData((List<Integer>)arrayWrapper.getWrappedData()).build()));
				break;
			case DT_UINT8:
				dataBuilder.setData(Any.pack(UInt8Array.newBuilder().setData(ByteString.copyFrom((byte[])ArrayUtil.toArray((List<Byte>)arrayWrapper.getWrappedData(), arrayWrapper.getShape(), byte.class, ""))).build()));
				break;
			case DT_STRING:
				dataBuilder.setData(Any.pack(StringArray.newBuilder().addAllData((List<String>)arrayWrapper.getWrappedData()).build()));
				break;
			case DT_INT64:
				dataBuilder.setData(Any.pack(Int64Array.newBuilder().addAllData((List<Long>)arrayWrapper.getWrappedData()).build()));
				break;
			case DT_BOOL:
				dataBuilder.setData(Any.pack(BoolArray.newBuilder().addAllData((List<Boolean>)arrayWrapper.getWrappedData()).build()));
				break;
			default:
				throw new IllegalArgumentException(String.format("Unsupported data type '%s'!", arrayWrapper.getType().name()));
			}
			builder.putInputs(entry.getKey(), dataBuilder.build());
		}
		return builder.build();
	}

	public static RequestInfo createRequestInfo(Sample sample, Param param)
	{
		if(param!=null)
			return RequestInfo.newBuilder().setSample(sample).setParam(param).build();
		else
			return RequestInfo.newBuilder().setSample(sample).build();
	}

	public static Map<String, ArrayWrapper4Pb> parseDataMap(Map<String, DataArray> dataMap)
	{
		Map<String, ArrayWrapper4Pb> res=new HashMap<>();
		for(Entry<String, DataArray> entry : dataMap.entrySet())
		{
			res.put(entry.getKey(), new ArrayWrapper4Pb(entry.getValue()));
		}
		return res;
	}

	public static Map<String, ArrayWrapper4Pb> parseResponse(Response response)
	{
		Status status=response.getStatus();
		if(status.getCode()==0)
			return parseDataMap(response.getDataMap());
		else
			throw new ResponseException(String.format("Failed response: status.code=%d, status.des=%s.", status.getCode(), status.getDes()));
	}

	public static String formatResponse(Response response)
	{
		Status status=response.getStatus();
		StringBuilder stringBuilder=new StringBuilder(String.format("status.code=%d, status.des=%s\n", status.getCode(), status.getDes()));
		Gson gson=new GsonBuilder().serializeSpecialFloatingPointValues().create();
		for(Entry<String, ArrayWrapper4Pb> entry : parseDataMap(response.getDataMap()).entrySet())
		{
			ArrayWrapper4Pb wrapper=entry.getValue();
			wrapper.getFeedData();
			stringBuilder.append(entry.getKey()+":"+gson.toJson(wrapper)).append('\n');
		}
		return stringBuilder.toString();
	}

	public TfGrpcClient(String host, int port, boolean plainText)
	{
		this(plainText ? ManagedChannelBuilder.forAddress(host, port).usePlaintext() : ManagedChannelBuilder.forAddress(host, port));
	}

	public TfGrpcClient(String target, boolean plainText)
	{
		this(plainText ? ManagedChannelBuilder.forTarget(target).usePlaintext() : ManagedChannelBuilder.forTarget(target));
	}

	/**
	 * Construct client for accessing the Tf GRPC server using the existing channel builder.
	 * 
	 * @param channelBuilder the builder for {@link ManagedChannel} instances.
	 */
	protected TfGrpcClient(ManagedChannelBuilder<?> channelBuilder)
	{
		channel=channelBuilder.build();
		asyncStub=TfServiceGrpc.newStub(channel);
		blockingStub=TfServiceGrpc.newBlockingStub(channel);
	}

	public void shutdown(long timeout, TimeUnit unit) throws InterruptedException
	{
		channel.shutdown().awaitTermination(timeout, unit);
	}

	/**
	 * Blocking unary call.
	 * 
	 * @param requestInfo the {@link RequestInfo} instance contains request information.
	 * @return the {@link Response} ProtoBuf data structure contains the response from the server.
	 */
	public Response request(RequestInfo requestInfo)
	{
		return blockingStub.request(requestInfo);
	}

	/**
	 * Asynchronous stream call.
	 * 
	 * @param <R> the type returned by the {@code responseHandler}.
	 * 
	 * @param sampleIterator an {@link Iterator} to provide {@link RequestInfo} instances.
	 * @param responseHandler a {@link Function} handler to deal with responses. 
	 * @return a {@link CountDownLatch} object indicates the request stream is finished when it counts to zero. 
	 */
	public <R> CountDownLatch requestStream(Iterator<RequestInfo> sampleIterator, Function<Response, R> responseHandler)
	{
		final CountDownLatch finishLatch=new CountDownLatch(1);
		StreamObserver<RequestInfo> requestObserver=asyncStub.requestStream(new StreamObserver<Response>() {
			@Override
			public void onNext(Response value)
			{
				responseHandler.apply(value);
			}

			@Override
			public void onError(Throwable t)
			{
				finishLatch.countDown();
			}

			@Override
			public void onCompleted()
			{
				finishLatch.countDown();
			}
		});

		try
		{
			while(sampleIterator.hasNext())
			{
				requestObserver.onNext(sampleIterator.next());
			}
		}
		catch(RuntimeException e)
		{
			// Cancel RPC
			requestObserver.onError(e);
			throw e;
		}
		// Mark the end of requests
		requestObserver.onCompleted();

		// return the latch while receiving happens asynchronously
		return finishLatch;
	}
}
