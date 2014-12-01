package mpi;

public class Comm {

	public int Rank() throws MPIException {
		return 0;
	}
	
	public void Send(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPIException {
		
	}
	
	public Status Recv(java.lang.Object buf, int offset, int count, Datatype datatype, int source, int tag) throws MPIException {
		return new Status();
	}
}
