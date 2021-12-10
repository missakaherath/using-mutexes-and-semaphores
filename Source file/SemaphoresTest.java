/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package semaphores;

import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;
import java.util.Scanner;

/**
 *
 * @author sasmitha
 */
public class SemaphoresTest {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        Semaphore strider = new Semaphore(0,true);
        Semaphore mutexCount = new Semaphore(1,true);
        Semaphore boarded = new Semaphore(0,true);
        
//        float riderArrivalMeanTime = 30f * 1000;
//        float busArrivalMeanTime = 20 * 60f * 1000 ;
        
        float riderArrivalMeanTime = 5f * 1000;
        float busArrivalMeanTime = 1 * 1f * 1000 ;
        
        Scanner scanner = new Scanner(System.in);
        String userInput;
        
        
        RiderGenerator riderGenerator = new RiderGenerator(riderArrivalMeanTime, mutexCount, strider, boarded);
        (new Thread(riderGenerator)).start();

        BusGenerator busGenerator = new BusGenerator(busArrivalMeanTime,mutexCount, strider, boarded);
        (new Thread(busGenerator)).start();
        
        while(true){
            userInput = scanner.nextLine();
            if(userInput != null)
                System.exit(0);
        }        
    }
    
}

class Counter{
    public static int count = 0;
    public static int remainder = 0;

}

class StriderThread extends Thread{
    Semaphore mutexCount;
    Semaphore strider;
    Semaphore boarded;
    
    public StriderThread (Semaphore mutexCount, Semaphore strider, Semaphore boarded){
        this.mutexCount = mutexCount;
        this.strider = strider;
        this.boarded = boarded;
    }
    
    @Override
    public void run(){
        try {
            this.mutexCount.acquire();
            Counter.count++;
            this.mutexCount.release();
            this.arrive();
            this.strider.acquire();
            this.boardBus();
            this.boarded.release();
            
        } catch (InterruptedException ex) {
            Logger.getLogger(StriderThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void boardBus(){
        System.out.println("  Boarded to bus :" + this.getName());
    }
    
    private void arrive(){
        System.out.println("Strider arrived "+ this.getName());
    }
}


class BusThread extends Thread{
    Semaphore mutexCount;
    Semaphore strider;
    Semaphore boarded;

    private int maxStriders = 50;
    private int remain, initialRemain;

    
    public BusThread (Semaphore mutexCount, Semaphore strider, Semaphore boarded){
        this.mutexCount = mutexCount;
        this.strider = strider;
        this.boarded = boarded;
    }
    
    @Override
    public void run(){
        try {
            this.mutexCount.acquire();
            this.arrive();
            if (Counter.count > this.maxStriders){
                Counter.count -= this.maxStriders;
                this.remain = this.maxStriders;
            }else{
                this.remain = Counter.count;
                Counter.count = 0;
            }
            initialRemain = remain;
            while(this.remain>0){
                this.strider.release();
                this.boarded.acquire();
                this.remain --;
            }
            
            this.mutexCount.release();
            
            this.depart(initialRemain);
            
                      
        } catch (InterruptedException ex) {
            Logger.getLogger(StriderThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void depart(int initialRemain){
        System.out.println("Departed the bus with "+initialRemain+" # of passengers " + this.getName());
    }
    
    private void arrive(){
        System.out.println("\nBus arrived "+ this.getName());
    }
    
}

class BusGenerator implements Runnable {

    private float arrivalMeanTime;
    private static Random random;
   
    Semaphore mutexCount;
    Semaphore strider;
    Semaphore boarded;

    public BusGenerator(float arrivalMeanTime, Semaphore mutexCount, Semaphore strider, Semaphore boarded) {
        this.arrivalMeanTime = arrivalMeanTime;
        random = new Random();
        this.mutexCount = mutexCount;
        this.strider = strider;
        this.boarded = boarded;
    }

    @Override
    public void run() {

        int busIndex = 1;

        // Spawning bus threads for the user specified value
        while (!Thread.currentThread().isInterrupted()) {

            try {
                // Initializing and starting the bus threads
                BusThread bus = new BusThread(mutexCount, strider, boarded);
                String busName = "Bus-"+Integer.toString(busIndex);
                bus.setName(busName);
                bus.start();

                busIndex++;
                // Sleeping the thread to obtain the inter arrival time between the bus threads
                Thread.sleep(getExponentiallyDistributedBusInterArrivalTime());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("All buses have finished arriving");
    }

    // Method to get the exponentially distributed bus inter arrival time
    public long getExponentiallyDistributedBusInterArrivalTime() {
        float lambda = 1 / arrivalMeanTime;
        return Math.round(-Math.log(1 - random.nextFloat()) / lambda);
    }
}

class RiderGenerator implements Runnable {

    private float arrivalMeanTime;
    private static Random random;
    Semaphore mutexCount;
    Semaphore strider;
    Semaphore boarded;

    public RiderGenerator(float arrivalMeanTime, Semaphore mutexCount, Semaphore strider, Semaphore boarded) {
        this.arrivalMeanTime = arrivalMeanTime;
        random = new Random();
        this.mutexCount = mutexCount;
        this.strider = strider;
        this.boarded = boarded;
    }

    @Override
    public void run() {

        int riderIndex = 1;
        // Spawning rider threads for the user specified value
        while (!Thread.currentThread().isInterrupted()) {

            try {
                // Initializing and starting the rider threads
                StriderThread rider = new StriderThread (mutexCount, strider, boarded);
                String striderName = "Strider-"+Integer.toString(riderIndex);
                rider.setName(striderName);
                rider.start();

                riderIndex++;
                // Sleeping the thread to obtain the inter arrival time between the threads
                Thread.sleep(getExponentiallyDistributedRiderInterArrivalTime());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public long getExponentiallyDistributedRiderInterArrivalTime() {
        float lambda = 1 / arrivalMeanTime;
        return Math.round(-Math.log(1 - random.nextFloat()) / lambda);
    }
}