import java.net.*;
import java.io.*;

class Main {

  public static void main( String args[] ) {
    Server server = new Server();
  }

}

class Server {
  final int port = 6341;
  Socket clientSocket;

  final int nRe = 3000;
  final int nIm = 2000;

  int[][] nIter = new int[nRe+1][nIm+1];

  public Server() {

    try {
      ServerSocket listenSocket = new ServerSocket( port );

      System.out.println("starting mandelbrot");
      for( int i=0; i<nRe+1; i++ ) {
        clientSocket = listenSocket.accept();
        MandelbrotConnection conn = new MandelbrotConnection( clientSocket, i );
        conn.start();
      }

      System.out.println("Gathering Results");
      while( true ) {
        clientSocket = listenSocket.accept();
        CollectionConnection conn = new CollectionConnection( clientSocket, this );
        conn.start();
      }

    } catch( IOException e ) {
      System.out.println( "Listen: "  + e.getMessage() );
    }

  }

  public void writeResults() {
    final float z_Re_min = -2f;
    final float z_Re_max =  1f;
    final float z_Im_min = -1f;
    final float z_Im_max =  1f;

    System.out.println("Writing results");
    try {
      FileWriter writer = new FileWriter( "mandelbrot.dat" );
      for ( int i=0; i<nRe+1; i++ ){
        for ( int j=0; j<nIm+1; j++ ){
          float z_Re = ( ( (float)i ) / ( (float)nRe ) ) *
                       (z_Re_max - z_Re_min) + z_Re_min;
          float z_Im = ( ( (float)j ) / ( (float)nIm ) ) *
                       (z_Im_max - z_Im_min) + z_Im_min;
          String line = String.format( "%f %f %d \n", z_Re, z_Im, nIter[i][j] );
          writer.write( line );
        }
      }
      writer.close();
    } catch ( IOException e ) {
      System.out.println( "Write error: " + e.getMessage() );
    }
    System.out.println("Finished Writing");
  }

  public void addToResults( String data ){
    String[] vals = data.split("-");
    int iVal = Integer.parseInt( vals[0] );
    int jVal = Integer.parseInt( vals[1] );
    int kVal = Integer.parseInt( vals[2] );
    nIter[iVal][jVal] = kVal;
  }

}

class MandelbrotConnection extends Thread {
  DataInputStream  in;
  DataOutputStream out;
  Socket           clientSocket;
  int              nextIter;

  public MandelbrotConnection( Socket clientSocket, int nextIter ) {
    try {
      this.clientSocket = clientSocket;
      in  = new DataInputStream ( clientSocket.getInputStream()  );
      out = new DataOutputStream( clientSocket.getOutputStream() );
      this.nextIter = nextIter;
    } catch( IOException e ) {
      System.out.println( "Connection: " + e.getMessage() );
    }
  }

  public void run() {
    try {
      out.writeInt( nextIter );
    } catch( EOFException e ) {
      System.out.println( "EOF: " + e.getMessage() );
    } catch( IOException e ) {
      System.out.println( "IO: " + e.getMessage() );
    } finally {

      try {
        clientSocket.close();
      } catch( IOException e ) {
        System.out.println( "close: " + e.getMessage() );
      }

    }
  }

}

class CollectionConnection extends Thread {
  DataInputStream  in;
  DataOutputStream out;
  Socket           clientSocket;
  Server           server;

  public CollectionConnection( Socket clientSocket, Server server ) {
    try {
      this.clientSocket = clientSocket;
      in  = new DataInputStream ( clientSocket.getInputStream()  );
      out = new DataOutputStream( clientSocket.getOutputStream() );
      this.server = server;
    } catch( IOException e ) {
      System.out.println( "Connection: " + e.getMessage() );
    }
  }

  public void run() {
    try {
      String initialReq = in.readUTF();
      System.out.println( "initial request: " + initialReq );
      if ( initialReq.equals("startColl") ) {
        int listSize = in.readInt();
        System.out.println( "Collecting list of size: " + Integer.toString(listSize) );
        for ( int i=0; i<listSize; i++ ){
          String nextRes = in.readUTF();
          server.addToResults( nextRes );
        }
        System.out.println("all added");
        out.writeInt( -1 );
      } else {
        if ( initialReq.equals("nextIter") ) {
          out.writeInt( -1 );
        } else if ( initialReq.equals("end") ) {
          server.writeResults();
          out.writeInt( -1 );
        }
      }
    } catch( EOFException e ) {
      System.out.println( "EOF: " + e.getMessage() );
    } catch( IOException e ) {
      System.out.println( "IO: " + e.getMessage() );
    } finally {

      try {
        clientSocket.close();
      } catch( IOException e ){
        System.out.println( "close: " + e.getMessage() );
      }

    }
  }
}
