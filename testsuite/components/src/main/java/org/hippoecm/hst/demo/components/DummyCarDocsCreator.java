/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.demo.components;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * <b>Note</b>: this is not the preferred way to normally persist data into the repository, as you would use workflow for it
 * through the hst persistency layer. This though serves just to create some amount of dummy content in a really fast way.  
 *
 */
public class DummyCarDocsCreator {

    
    private final static String[] COLORS = {"red","grey", "green", "blue", "black"};
    
    public void createCars(Session writableSession, String rootByPath, int number) throws RepositoryException{
        
        List<Car> availableCars = new ArrayList<Car>();
        
        fill(availableCars);
        
        Node baseNode = writableSession.getRootNode().getNode(rootByPath);
       
        Node productsFolder;
        
        if(!baseNode.hasNode("products")) {
            productsFolder = baseNode.addNode("products", "hippostd:folder");
            productsFolder.addMixin("mix:referenceable");
        } else {
            productsFolder = baseNode.getNode("products");
        }
        
        Node dummyProducts = null;
        dummyProducts  = productsFolder.addNode("dummy-"+System.currentTimeMillis(), "hippostd:folder");
        dummyProducts.addMixin("mix:referenceable");
        
        
        Random r = new Random();
        
        Calendar cal = Calendar.getInstance();
        Node crFolder = dummyProducts.addNode("folder"+cal.getTimeInMillis());
        crFolder.addMixin("mix:referenceable");
        
        for(int i = 0 ;i < number; i++) {
            cal = Calendar.getInstance();
            
            int car = r.nextInt(availableCars.size());
            Car randomCar = availableCars.get(car);
            
            Node handle = crFolder.addNode(randomCar.brand + "-" + cal.getTimeInMillis(), "hippo:handle"); 
            handle.addMixin("mix:referenceable");
            handle.addMixin("hippo:translated");
            
            Node translation = handle.addNode("hippo:translation","hippo:translation");
            translation.setProperty("hippo:message", randomCar.brand + "-" + cal.getTimeInMillis());
            translation.setProperty("hippo:language", "");
            
            Node doc = handle.addNode(randomCar.brand + "-"+cal.getTimeInMillis(), "demosite:productdocument");
            doc.addMixin("mix:referenceable");
            
            doc.setProperty("demosite:created", Calendar.getInstance());
            
            int monthBack = -r.nextInt(200);
            int dayBack = -r.nextInt(30);
            int hourBack = -r.nextInt(24);
            int minuteBack = -r.nextInt(60);
            cal.add(Calendar.MONTH, monthBack);
            cal.add(Calendar.DAY_OF_YEAR, dayBack);
            cal.add(Calendar.HOUR, hourBack);
            cal.add(Calendar.MINUTE, minuteBack);
            doc.setProperty("demosite:constructiondate", cal);
            
            doc.setProperty("demosite:product", "car");
            doc.setProperty("demosite:brand", randomCar.brand);
            
            int color = r.nextInt(COLORS.length);
            doc.setProperty("demosite:color", COLORS[color]);
            
            double price = r.nextDouble()*(randomCar.upperPrice - randomCar.lowerPrice) + randomCar.lowerPrice;
            doc.setProperty("demosite:price", ((Long)Math.round(price)).doubleValue());
            
            long mileage = r.nextInt((int)(randomCar.upperMileage-randomCar.lowerMileage)) + randomCar.lowerMileage;
            doc.setProperty("demosite:mileage", mileage);
            String[] availability = {"live", "preview"};
            doc.setProperty("hippo:availability", availability );
            doc.setProperty("hippostd:stateSummary", "live");
            doc.setProperty("hippostd:state", "published");
            doc.setProperty("hippostdpubwf:lastModifiedBy", writableSession.getUserID());
            doc.setProperty("hippostdpubwf:createdBy", writableSession.getUserID());
            doc.setProperty("hippostdpubwf:lastModificationDate",  Calendar.getInstance());
            doc.setProperty("hippostdpubwf:creationDate", Calendar.getInstance() );
            doc.setProperty("hippostdpubwf:publicationDate", Calendar.getInstance() );
            
            // add tags:
            
            doc.addMixin("hippostd:taggable");
            int randomNumberOfTagsSeterOfTags = 0;
            
            
            Set<String> randomTags = new HashSet<String>();
            randomTags.add(randomCar.brand);
            if(randomCar.defaultTags != null) {
                if(randomCar.defaultTags.length > 0) {
                    randomNumberOfTagsSeterOfTags= r.nextInt(randomCar.defaultTags.length) + 1;
                    if(randomNumberOfTagsSeterOfTags > randomCar.defaultTags.length) {
                        randomNumberOfTagsSeterOfTags = randomCar.defaultTags.length;
                    }
                }
                for(int j = 0; j < randomNumberOfTagsSeterOfTags;) {
                    if(randomTags.add(randomCar.defaultTags[r.nextInt(randomNumberOfTagsSeterOfTags)])) {
                        j++;
                    } else {
                        // tag was already set, skip
                    } 
                }
            }
            
            doc.setProperty("hippostd:tags", randomTags.toArray(new String[randomTags.size()]));
            
            Node body = doc.addNode("demosite:body","hippostd:html");
            body.setProperty("hippostd:content", "<html> <body> <p>body </p> </body> </html>");
            
            if(i%100 == 0) {
                writableSession.save();
                crFolder = dummyProducts.addNode("folder"+cal.getTimeInMillis());
                crFolder.addMixin("mix:referenceable");
            }
        }
        writableSession.save();
    }
    
    
    private void fill(List<Car> availableCars) {
        addCars(availableCars, "Alfa Romeo", 8, 10000, 20000, 1, 200000, new String[] {"Italian", "FIAT Group", "sportsmanlike", "said", "large company"});
        addCars(availableCars, "Aston Martin", 2, 10000, 100000, 1, 200000, new String[] {"English", "david richards", "james bond"});
        addCars(availableCars, "Audi", 6, 10000, 100000, 1, 200000, new String[] {"German", "high quality", "Ingolstadt", "Volkswagon group", "August Horch", "expensive", "fast"});
        addCars(availableCars, "Austin", 3, 10000, 100000, 1, 200000, new String[]{"English", "Birmingham", "Morris", "BMC"});
        addCars(availableCars, "Bentley", 2, 30000, 400000, 1, 400000, new String[] {"English", "expensive", "fast", "luxury", "sportswagon"});
        addCars(availableCars, "BMW", 6, 10000, 200000, 1, 200000, new String[] {"Bayerische Motoren Werke","high quality" ,"german", "expensive", "fast", "V12", "Rapp"});
        addCars(availableCars, "Buick", 2, 10000, 200000, 1, 200000, new String[] {"US", "General Motors", "big three US"});
        addCars(availableCars, "Cadillac", 2, 10000, 200000, 1, 200000, new String[] {"US", "luxury", "high quality", "Henry Ford","Leland", "William Durant"});
        addCars(availableCars, "Chevrolet", 4, 10000, 40000, 1,200000, new String[] {"US", "General Motors", "large quantity","William Durant", "Louis Chevrolet", "big three US"});
        addCars(availableCars, "Chrysler", 4, 10000, 50000, 1, 200000,new String[] {"US", "Walter Chrysler", "big three US", "Daimler-Benz","DaimlerChrysler", "Cerberus", "Fiat"});
        addCars(availableCars, "Citroen", 8, 10000, 30000, 1, 200000,new String[] {"French", "Mors", "Ssalomon", "cheap"});
        addCars(availableCars, "Daewoo", 6, 10000, 30000, 1, 200000, new String[] {"South-Korean", "General Motors", "Saenara Motor", "GM Deawoo" });
        addCars(availableCars, "Daihatsu", 6, 10000, 30000, 1, 200000, new String[] {"Japanese", "kei cars", " Hatsudoki", "Toyota", "cheap",});
        addCars(availableCars, "Ferrari", 1, 40000, 900000, 1, 300000, new String[] {"Italian", "FIAT Group", "exclusive", "sportsmanlike", "fast", "Enzo Ferrari"});
        addCars(availableCars, "Fiat", 8, 3000, 25000, 1, 200000, new String[] {"Italian", "FIAT Group", "cheap", "innovative", "large company"});
        addCars(availableCars, "Ford", 7, 3000, 30000, 1, 200000, new String[] {"US", "big three US", "Ford Motor Company", "Volvo", "Lincoln", "Mercury", "large company", "innovative", "cheap" });
        addCars(availableCars, "Honda", 7, 20000, 30000, 1, 200000, new String[] {"Japanese", "motorcycles", "large company", "cheap", "big Asian four"});
        addCars(availableCars, "Hummer", 1, 50000, 300000, 1, 200000, new String[] {"US", "off-road", "General Motors", "army", "large company" });
        addCars(availableCars, "Hyundai", 7, 10000, 25000, 1, 200000, new String[] {"South-Korean", "Kia", "large company", "big Asian four"});
        addCars(availableCars, "Jaguar", 4, 10000, 100000, 1, 200000, new String[] {"English", "luxery", "Ford", "large company", "expensive", "high quality"});
        addCars(availableCars, "Jeep", 4, 10000, 70000, 1, 200000, new String[] {"US", "off-road", "Chrysler", "Nekaf", "army", "SUV"});
        addCars(availableCars, "Kia", 6, 1000, 20000, 1, 200000, new String[] {"South-Korean", "Hyundai Motor Company", "motorcycles", "large company", "cheap"});
        addCars(availableCars, "Lada", 5, 1000, 20000, 1, 200000, new String[] {"Russian", "AvtoVAZ", "large company", "cheap"});
        addCars(availableCars, "Lamborghini", 3, 90000, 400000, 1, 200000, new String[] {"Italian", "Ferruccio Lamborghini", "fast", "sportswagon", "expensive", "high quality", "Volkswagon group"});
        addCars(availableCars, "Lancia", 5, 40000, 200000, 1, 200000, new String[] {"Italian", "Vincenzo Lancia", "FIAT Group", });
        addCars(availableCars, "Land Rover", 5, 10000, 20000, 1, 200000, new String[] {"English", "SUV", "Tata Motors"});
        addCars(availableCars, "Lexus", 5, 100000, 200000, 1, 200000, new String[] {"Japanese", "Toyota", "luxury", "fast", "expensive", "high quality"});
        addCars(availableCars, "Lincoln", 5, 40000, 200000, 1, 200000, new String[] {"US", "Ford Motor Company", "luxury", "expensive"});
        addCars(availableCars, "Lotus", 5, 40000, 200000, 1, 200000, new String[] {"English", "luxury", "sportswagon"});
        addCars(availableCars, "Maserati", 3, 40000, 200000, 1, 200000, new String[] {"Italian", "sportswagon", "Alfa Romeo", "FIAT Group", "motorcycles"});
        addCars(availableCars, "Mazda", 8, 3000, 30000, 1, 200000, new String[] {"Japanese", "motorcycles", "large company"});
        addCars(availableCars, "Mercedes-Benz", 6, 3000, 40000, 1, 200000, new String[] {"German", "Daimler AG", "Team McLaren", "large company", "luxury"});
        addCars(availableCars, "Mercury", 3, 3000, 40000, 1, 200000, new String[] {"US", "SUV", "army", "Ford Motor Company"});
        addCars(availableCars, "Mini", 6, 3000, 40000, 1, 200000, new String[] {"English", "BMW", "very small"});
        addCars(availableCars, "Mitsubishi", 6, 3000, 20000, 1, 200000, new String[] {"Japenese", "Mitsubishi Group", "large company"});
        addCars(availableCars, "Nissan", 7, 3000, 30000, 1, 200000, new String[] {"Japanese", "large company", "Nissan Skyline", "big Asian four"});
        addCars(availableCars, "Opel", 8, 3000, 40000, 1, 200000, new String[] {"German", "General Motors", "big three US", "Vauxhall", "motorcycles"});
        addCars(availableCars, "Peugeot", 9, 3000, 30000, 1, 200000, new String[] {"French", "motorcycles", "large company", "bicycle"});
        addCars(availableCars, "Pontiac", 5, 70000, 200000, 1, 200000, new String[] {"US", "General Motors", "large company", "sportswagon"});
        addCars(availableCars, "Porsche", 4, 60000, 300000, 1, 200000, new String[] {"German", "sportswagon", "bicycle", "fast"});
        addCars(availableCars, "Renault", 9, 3000, 20000, 1, 200000, new String[] {"French", "cheap", "Voiturette", "large company"});
        addCars(availableCars, "Rolls-Royce", 2, 13000, 500000, 1, 200000, new String[] {"German", "BMW", "expensive", "luxury"});
        addCars(availableCars, "Rover", 4, 3000, 40000, 1, 200000, new String[] {"English", "bankrupt", "bicycle", "motorcycles", "Land Rover"});
        addCars(availableCars, "Saab", 6, 3000, 60000, 1, 200000, new String[] {"Swedish", "General Motors", "big three US", "sportswagon", "innovative" });
        addCars(availableCars, "Seat", 6, 3000, 20000, 1, 200000, new String[] {"Spanish", "cheap", "Volkswagon group"});
        addCars(availableCars, "Skoda", 6, 3000, 20000, 1, 200000, new String[] {"Czech", "bicycles", "cheap", "Volkswagon group"});
        addCars(availableCars, "Smart", 6, 3000, 20000, 1, 200000, new String[] {"German", "smart centers", "Daimler AG", "very small", "large company"});
        addCars(availableCars, "Ssangyong", 5, 3000, 20000, 1, 200000, new String[] {"South-Korean", "army", "jeep"});
        addCars(availableCars, "Subaru", 6, 3000, 20000, 1, 200000, new String[] {"Japanese", "Fuji Heavy Industries", "innovative"});
        addCars(availableCars, "Suzuki", 6, 3000, 15000, 1, 200000, new String[] {"Janapese", "motorcycles", "cheap"});
        addCars(availableCars, "Toyota", 8, 3000, 45000, 1, 200000, new String[] {"Japanese", "large company", "Hybrid", "big Asian four"});
        addCars(availableCars, "Triumph", 5, 3000, 15000, 1, 200000, new String[] {"English", "bicycles", "BMW", "sportswagon"});
        addCars(availableCars, "Volkswagen", 9, 3000, 75000, 1, 200000, new String[] {"German", "large company", "cheap", "Volkswagon group"});
        addCars(availableCars, "Volvo", 8, 3000, 75000, 1, 200000, new String[] {"Swedish", "Ford Motor Company", "luxury", "high quality"});
        addCars(availableCars, "Overige Autos", 1, 3000, 75000, 1, 200000, new String[] {"Imaginary", "random", "cheap"});

    }
    
    private void addCars(List<Car> availableCars, String brand, int ocurrences, double lowerPrice, double upperPrice, long lowerMileage, long upperMileage) {
        this.addCars(availableCars, brand, ocurrences, lowerPrice, upperPrice, lowerMileage, upperMileage, null);
    }

    private void addCars(List<Car> availableCars, String brand, int ocurrences, double lowerPrice, double upperPrice, long lowerMileage, long upperMileage, String[] defaultTags) {
        for(int i = 0; i < ocurrences; i++) {
            availableCars.add(new Car(brand, lowerPrice, upperPrice, lowerMileage, upperMileage, defaultTags));
        }
    }



    class Car {
        String brand;
        double lowerPrice;
        double upperPrice;
        long lowerMileage;
        long upperMileage;
        String[] defaultTags;
        
        Car(String brand, double lowerPrice, double upperPrice, long lowerMileage, long upperMileage, String[] defaultTags) {
            this.brand = brand;
            this.lowerPrice = lowerPrice;
            this.upperPrice = upperPrice;
            this.lowerMileage = lowerMileage;
            this.upperMileage = upperMileage;
            this.defaultTags = defaultTags;
        }
    }

}
