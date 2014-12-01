package mpi;

public class MPI {

	public static Intracomm COMM_WORLD;
	public static final int NUM_OF_PROCESSORS = 1;
	public static int UNDEFINED;
	public static Datatype NULL;
	public static Datatype BYTE;
	public static Datatype CHAR;
	public static Datatype SHORT;
	public static Datatype BOOLEAN;
	public static Datatype INT;
	public static Datatype LONG;
	public static Datatype FLOAT;
	public static Datatype DOUBLE;
	public static Datatype PACKED;
	public static Datatype LB;
	public static Datatype UB;
	public static Datatype OBJECT;
	public static int THREAD_SINGLE;
	public static int THREAD_FUNNELED;
	public static int THREAD_SERIALIZED;
	public static int THREAD_MULTIPLE;
	public static Datatype SHORT2;
	public static Datatype INT2;
	public static Datatype LONG2;
	public static Datatype FLOAT2;
	public static Datatype DOUBLE2;
	public static Op MAX;
	public static Op MIN;
	public static Op SUM;
	public static Op PROD;
	public static Op LAND;
	public static Op BAND;
	public static Op LOR;
	public static Op BOR;
	public static Op LXOR;
	public static Op BXOR;
	public static Op MAXLOC;
	public static Op MINLOC;
	public static int ANY_SOURCE;
	public static int ANY_TAG;
	public static Status EMPTY_STATUS;
	public static int PROC_NULL;
	public static int BSEND_OVERHEAD;
	public static int SEND_OVERHEAD;
	public static int RECV_OVERHEAD;
	
	public MPI() {
		
	}
	
	public static String[] Init(String[] argv) throws MPIException {
		return new String[0];
	}
	
	public static void Finalize() throws MPIException {
		
	}
}
