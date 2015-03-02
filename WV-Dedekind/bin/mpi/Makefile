# Generated automatically from Makefile.in by configure.
# File         : Makefile
# Author       : Sung-Hoon Ko, Xinying Li
# Created      : Thu Apr 1998
# Revision     : ver 1.1
# Updated      : $Date: 2003/01/17 02:44:48 $
# Copyright: Northeast Parallel Architectures Center at Syracuse University 1998

SHELL       = /bin/sh


srcdir = .
top_srcdir = ../..

VPATH = .:${srcdir}


CC = /mirror/mpich2/bin/mpicc

JDK = /usr/lib/jvm/java-7-oracle

MPI_INC = -I/mirror/mpich2/include
MPI_LIB = -L/mirror/mpich2/lib

CCLIBKEY = 

DEFPINS = 
DEFUNPACK = -DUNPACK_ALLOWS_SHORT_BUFFER
DEFJVMSIGNAL1 = 

INCLUDES = -I$(JDK)/include -I$(JDK)/include/linux $(MPI_INC)

CLASSES = ../../lib/classes
LIB = ../../lib

TITLE = mpi
PFX = $(TITLE)_
PKG = $(TITLE).
DIR = $(TITLE)/

RM = rm
CP = cp
MV = mv

JC = $(JDK)/bin/javac
JH = $(JDK)/bin/javah
JFLAG = -jni
CFLAGS = -c $(DEFPINS) $(DEFUNPACK)

LD = /mirror/mpich2/bin/mpicc
LDFLAG = -shared
LDFLAGSIG = -shared

OBJ = $(PFX)MPI.o       $(PFX)Comm.o      \
      $(PFX)Op.o        $(PFX)Datatype.o  \
      $(PFX)Intracomm.o $(PFX)Intercomm.o \
      $(PFX)Cartcomm.o  $(PFX)Graphcomm.o \
      $(PFX)Group.o     $(PFX)Status.o    \
      $(PFX)Request.o   \
      $(PFX)Errhandler.o


all:: $(LIB)/lib$(TITLE).so $(LIB)/libsavesignals.so


$(LIB)/lib$(TITLE).so : $(OBJ)
	$(RM) -f $(LIB)/libmpijava.so
	$(LD) $(LDFLAG) -o $(LIB)/libmpijava.so \
                $(MPI_LIB) $(OBJ) $(CCLIBKEY); 

$(LIB)/libsavesignals.so : libsavesignals.c
	$(RM) -f $(LIB)/libsavesignals.so
	cc $(CFLAGS) $(INCLUDES) $(DEFJVMSIGNAL1) libsavesignals.c
	cc $(LDFLAGSIG) -o $(LIB)/libsavesignals.so libsavesignals.o 

.SUFFIXES:
.SUFFIXES: .c .o

.c.o:
	$(CC) $(CFLAGS) $(INCLUDES)  -o $@ $*.c



$(PFX)MPI.o :        $(PFX)MPI.c $(PFX)MPI.h mpiJava.h
$(PFX)Comm.o :       $(PFX)Comm.c $(PFX)Comm.h mpiJava.h
$(PFX)Intercomm.o :  $(PFX)Intercomm.c $(PFX)Intercomm.h
$(PFX)Intracomm.o :  $(PFX)Intracomm.c $(PFX)Intracomm.h mpiJava.h
$(PFX)Status.o :     $(PFX)Status.c $(PFX)Status.h mpiJava.h
$(PFX)Op.o :         $(PFX)Op.c $(PFX)Op.h
$(PFX)Request.o :    $(PFX)Request.c $(PFX)Request.h mpiJava.h
$(PFX)Prequest.o :   $(PFX)Prequest.c $(PFX)Prequest.h
$(PFX)Datatype.o :   $(PFX)Datatype.c $(PFX)Datatype.h
$(PFX)Cartcomm.o :   $(PFX)Cartcomm.c $(PFX)Cartcomm.h
$(PFX)Graphcomm.o :  $(PFX)Graphcomm.c $(PFX)Graphcomm.h
$(PFX)Group.o :      $(PFX)Group.c $(PFX)Group.h
$(PFX)Errhandler.o : $(PFX)Errhandler.c $(PFX)Errhandler.h

clean :
	$(RM) -f *.o 
	$(RM) -f $(LIB)/lib*.so
#	$(RM) -f $(PFX)*.h
