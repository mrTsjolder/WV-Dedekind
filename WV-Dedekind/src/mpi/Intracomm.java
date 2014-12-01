package mpi;

public class Intracomm extends Comm{

	/**
	 * Each process sends the contents of its send buffer to the root process.
	 * 
	 * @param 	sendbuf
	 * 			send buffer array
	 * @param 	sendoffset
	 * 			initial offset in send buffer
	 * @param 	sendcount
	 * 			number of items to send
	 * @param 	sendtype
	 * 			datatype of each item in send buffer
	 * @param 	recvbuf
	 * 			receive buffer array
	 * @param 	recvoffset
	 * 			initial offset in receive buffer
	 * @param 	recvcount
	 * 			number of items to receive
	 * @param 	recvtype
	 * 			datatype of each item in receive buffer
	 * @param 	root
	 * 			rank of receiving process
	 * @throws MPIException
	 */
	public void Gather(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype, Object recvbuf, 
			int recvoffset, int recvcount, Datatype recvtype, int root) throws MPIException {
		
	}
	
	/**
	 * Inverse of the operation Gather.
	 * 
	 * @param 	sendbuf
	 * 			send buffer array
	 * @param 	sendoffset
	 * 			initial offset in send buffer
	 * @param 	sendcount
	 * 			number of items to send
	 * @param 	sendtype
	 * 			datatype of each item in send buffer
	 * @param 	recvbuf
	 * 			receive buffer array
	 * @param 	recvoffset
	 * 			initial offset in receive buffer
	 * @param 	recvcount
	 * 			number of items to receive
	 * @param 	recvtype
	 * 			datatype of each item in receive buffer
	 * @param 	root
	 * 			rank of sending process
	 * @throws MPIException
	 */
	public void Scatter(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype, Object recvbuf,
			int recvoffset, int recvcount, Datatype recvtype, int root) throws MPIException {
		
	}
	
	
	/**
	 * Similar to Gather, but all processes receive the result.
	 * 
	 * @param 	sendbuf
	 * 			send buffer array
	 * @param 	sendoffset
	 * 			initial offset in send buffer
	 * @param 	sendcount
	 * 			number of items to send
	 * @param 	sendtype
	 * 			datatype of each item in send buffer
	 * @param 	recvbuf
	 * 			receive buffer array
	 * @param 	recvoffset
	 * 			initial offset in receive buffer
	 * @param 	recvcount
	 * 			number of items to receive
	 * @param 	recvtype
	 * 			datatype of each item in receive buffer
	 * @throws MPIException
	 */
	public void Allgather(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype, Object recvbuf, 
			int recvoffset, int recvcount, Datatype recvtype) throws MPIException {
		
	}

	/**
	 * Extension of Allgather to the case where each process sends distinct data to each of the receivers.
	 * 
	 * @param 	sendbuf
	 * 			send buffer array
	 * @param 	sendoffset
	 * 			initial offset in send buffer
	 * @param 	sendcount
	 * 			number of items to send
	 * @param 	sendtype
	 * 			datatype of each item in send buffer
	 * @param 	recvbuf
	 * 			receive buffer array
	 * @param 	recvoffset
	 * 			initial offset in receive buffer
	 * @param 	recvcount
	 * 			number of items to receive
	 * @param 	recvtype
	 * 			datatype of each item in receive buffer
	 * @throws MPIException
	 */
	public void Alltoall(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype, Object recvbuf,
			int recvoffset, int recvcount, Datatype recvtype) throws MPIException {
		
	}
	
	/**
	 * Combine elements in input buffer of each process using the reduce operation, 
	 * and return the combined value in the output buffer of the root process.
	 * 
	 * @param 	sendbuf	
	 * 			send buffer array
	 * @param	sendoffset	
	 * 			initial offset in send buffer
	 * @param	recvbuf	
	 * 			receive buffer arra
	 * @param	recvoffset	
	 * 			initial offset in receive buffer
	 * @param	count	
	 * 			number of items in send buffer
	 * @param	datatype	
	 * 			data type of each item in send buffer	
	 * @param	op	
	 * 			reduce operation
	 * @param	root	
	 * 			rank of root process
	 * @throws MPIException
	 */
	public void Reduce(Object sendbuf, int sendoffset, Object recvbuf, int recvoffset, int count, 
			Datatype datatype,  Op op, int root) throws MPIException {
		
	}
}
