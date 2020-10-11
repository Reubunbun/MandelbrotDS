//CANDIDATE 114765

import java.net.*;
import java.io.*;
import java.util.ArrayList;

class Main {

  public static void main( String args[] ) {
    final int    port    = 6341;
    final String address = "localhost";

    int numThreads = 4;
    Client[] clients = new Client[numThreads];

    //Create
    for( int i=0; i<numThreads; i++ ){
      clients[i] = new Client( address, port );
    }
    //Start
    for( int i=0; i<numThreads; i++ ){
      clients[i].start();
    }
    //Join
    for( int i=0; i<numThreads; i++ ){
      try {
        clients[i].join();
      } catch( Exception e ) {
        System.out.println( "Join: " + e.getMessage() );
      }
    }
    //End
    Client endClient = new Client( address, port );
    System.out.println("All clients finished");
    endClient.sendRequest( "end" );

  }

}

class Client extends Thread {
  String address;
  int    port;

  final   int maxIter  =  200;
  final   int nRe      = 3000;
  final   int nIm      = 2000;
  final float z_Re_min =  -2f;
  final float z_Re_max =   1f;
  final float z_Im_min =  -1f;
  final float z_Im_max =   1f;

  int[][] nIter = new int[nRe+1][nIm+1];

  public Client( String address, int port ) {
    this.address = address;
    this.port    = port;
  }

  public void run() {

    System.out.println("Client Calculating Results");
    int i = sendRequest( "nextIter" );

    while( true ) {
      int currentIter = i;
      i = getNextIter( currentIter );
      if ( i == -1 ) break;
    }

    System.out.println("Client packing results");
    ArrayList<String> packed = new ArrayList<String>();
    for ( i=0; i<nRe+1; i++ ){
      for ( int j=0; j<nIm+1; j++ ){
        int k = nIter[i][j];
        if ( k != 0 ) {
          String nextVal = Integer.toString(i) + "-" + Integer.toString(j) + "-" + Integer.toString(k);
          packed.add( nextVal );
        }
      }
    }

    System.out.println("Client Sending Results");
    sendResults( packed );

    System.out.println("Client finished");

  }

  public int getNextIter( int currentIter ) {
    for ( int j=0; j<nIm+1; j++ ) {
      float z0_Re  = ( ( (float)currentIter ) / ( (float)nRe ) ) *
                    (z_Re_max - z_Re_min) + z_Re_min;
      float z0_Im  = ( ( (float)j ) / ( (float)nIm ) ) *
                    (z_Im_max - z_Im_min) + z_Im_min;
      float z_Re = z0_Re;
      float z_Im = z0_Im;

      int k = 0;
      while ( k < maxIter ) {
        nIter[currentIter][j] = k;
        float z_Re_sq = z_Re*z_Re - z_Im*z_Im;
        float z_Im_sq = (float) 2f*z_Re*z_Im;
        float mod_z_sq_sq = z_Re_sq*z_Re_sq + z_Im_sq*z_Im_sq;
        if ( mod_z_sq_sq > 4 ) break;
        z_Re = z_Re_sq + z0_Re;
        z_Im = z_Im_sq + z0_Im;
        k++;
      }
    }
    return sendRequest( "nextIter" );
  }

  public int sendRequest( String message ) {
    int    resp = 0;
    Socket s    = null;

    try {
      s = new Socket( address, port );
      DataInputStream  in  = new DataInputStream ( s.getInputStream()  );
      DataOutputStream out = new DataOutputStream( s.getOutputStream() );
      out.writeUTF( message );

      resp = in.readInt();
    }catch( UnknownHostException e ) {
      System.out.println( "Sock: " + e.getMessage() );
    } catch( EOFException e ) {
      System.out.println( "EOF: " + e.getMessage() );
    } catch( IOException e ) {
      System.out.println( "IO: " + e.getMessage() );
    } finally {
      if ( s != null ) {
        try{
          s.close();
        } catch( IOException e ){
          System.out.println( "close: " + e.getMessage() );
        }
      }
    }
    return resp;
  }

  public void sendResults( ArrayList<String> results ){
    Socket s = null;

    try {
      s = new Socket( address, port );
      DataOutputStream out = new DataOutputStream( s.getOutputStream() );

      out.writeUTF("startColl");
      int listSize = results.size();
      out.writeInt( listSize );
      for( String vals: results ) {
        out.writeUTF( vals );
      }
      DataInputStream in = new DataInputStream( s.getInputStream() );
      int resp = in.readInt();
      if ( resp == -1 ) {
        System.out.println("Server notified about finishing");
        return;
      }

    }catch( UnknownHostException e ) {
      System.out.println( "Sock: " + e.getMessage() );
    } catch( EOFException e ) {
      System.out.println( "EOF: " + e.getMessage() );
    } catch( IOException e ) {
      System.out.println( "IO: " + e.getMessage() );
    } finally {
      if ( s != null ) {
        try {
          s.close();
        } catch( IOException e ){
          System.out.println( "close: " + e.getMessage() );
        }
      }
    }
  }

}
