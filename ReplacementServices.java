package org.openmetromaps.maps;

import java.util.ArrayList;
import java.util.List;

import org.openmetromaps.maps.model.Line;
import org.openmetromaps.maps.model.ModelData;
import org.openmetromaps.maps.model.Station;
import org.openmetromaps.maps.model.Stop;

public class ReplacementServices {

    private ReplacementServices() {}

    //Refaktor kód
    public static int calculateReplacementCost(ModelData model, Line line) {
        throw new RuntimeException("Not implemented");
    }
    
    public static void closeStation(ModelData model, Station station, List<Line> lines) {

        for(Line line : lines)
        {
            //Megkeressük hogy az adott "station"-ön megálló vonalakban benne van-e a törölni kivánt megálló, ha igen töröljük
            List<Stop> lineStops = line.getStops();
            Stop stoptoRemove = null;
            for(Stop stop : lineStops)
            {
                if(stop.getStation().equals(station))
                {
                    stoptoRemove = stop; 
                    break;
                }
            }
            if(lineStops.size() > 2 && stoptoRemove != null){ //Ha a vonalon kettőnél kevesebb "stop" maradna akkor nem végezzük el a műveletet
                lineStops.remove(stoptoRemove);
                station.getStops().remove(stoptoRemove);
                line.setStops(lineStops);
            }
            if(station.getStops().isEmpty()) model.stations.remove(station); //Ha a megállóban egyetlen vonal sem áll meg akkor töröljük a térképről
        }
    }
    
    public static void createReplacementService(ModelData model, List<Station> stations, List<Line> lines) {
        //1.feladat eleje
        if(isSelectionValid(stations, lines) && stations.size() >= 2 && linesHaveEnoughStops(lines)) //legalább kettő kiválasztott, egymás után következnek
        {
            for(Line line : lines)
            {
                boolean isStartEndStation = isEndStation(stations.getFirst(),line.getStops());  
                boolean isLastEndStation = isEndStation(stations.getLast(),line.getStops());
                if(isStartEndStation && isLastEndStation) return;
            }

            //Új potlóvonal
            String lineName;
            if(lines.size() == 1) //Hogy ha csak 1 vonalat választottunk ki
            {     
                lineName = "P"+lines.getFirst().getName();  //lehet getFirst mert alapbol 1 van benne
            }
            else //Több vonalat választottunk ki
            {
                lineName = "P-" + (replacementLineCount(model)+1);
            }
            List<Stop> newLineStops =  new ArrayList<>();
            Line newReplacementLine = new Line(replacementLineCount(model)+1,lineName,"#009EE3",false,newLineStops);
            for (Station station : stations) { //hozzáadjuk a kiválasztott szakasz stopjait
                for (Line line : lines) {
                    for(Stop stop: line.getStops())
                    {
                        if (stop.getStation().equals(station)) {
                            //Az új stopnak a line-ját a pótlóvonalra állitjuk be
                            Stop newStop = new Stop(station,newReplacementLine);    
                            //Megnézzük hogy benne-e van már az új vonalban az adott stop, ha nem akkor nem adjuk hozzá
                            boolean alreadyContains = newLineStops.stream().anyMatch(existingStop -> existingStop.getStation().equals(station));
                            if(!alreadyContains)
                            {
                                station.getStops().add(newStop);
                                newLineStops.add(newStop);
                            }
                        }
                    }
                } 
            }
            newReplacementLine.setStops(newLineStops);
            
           
            if(newReplacementLine.getStops().size() >= 2)
            {
                model.lines.add(newReplacementLine);
            }
            

            for(Line line : lines)
            {
                Station h1 = stations.getFirst(); //első szakaszhatár
                Station h2 = stations.getLast(); //második szakaszhatár
                List<Stop> stops = line.getStops();
                if(stops.size() >= 2) //6.feladat legalább 2 megálló
                {
                //Megnézzük hogy a szakaszhatárok végállomások-e
                boolean isStartEndStation = isEndStation(h1,stops);  
                boolean isLastEndStation = isEndStation(h2,stops);
                if(isStartEndStation && isLastEndStation) break; // iii. rész
                if(isStartEndStation || isLastEndStation) 
                {
                    Station newEndStation = isStartEndStation ? h2 : h1; //A nem végállomás szakaszhatár lesz az új végállomás
                    List<Stop> newStops = new ArrayList<>();
           
                    boolean addstops = true;
                    if(newEndStation.equals(h2)){ //ha a második szakaszhatár az új végállomás
                        for(Stop stop : stops)
                        {
                            //Addig adunk hozzá a potlóvonalhoz amig el nem érjük az új végállomást
                            if(stop.getStation().equals(newEndStation)) addstops = false;
                            if(addstops)
                            {
                                newStops.add(stop);
                            } 
                           
                        }
                        //Eltávolitjuk az eredeti vonal stopjait
                        removeOldStops(newStops,line);  
                    }
                    else
                    {
                         addstops = false;
                         for(Stop stop : stops)
                        {
                            //Addig nem adunk hozzá amig el nem értük az új végállomást
                            if(addstops) newStops.add(stop);
                            if(stop.getStation().equals(newEndStation)) addstops = true;    
                        }
                         //Eltávolitjuk az eredeti vonal stopjait
                         removeOldStops(newStops,line);
                    }
                 }
                if(!isStartEndStation && !isLastEndStation) {
                    int h1Index = getIndexOfBoundary(stops, h1); //első szakaszhatár megálló index
                    int h2Index = getIndexOfBoundary(stops, h2); //második szakaszhatár megálló index
                    boolean addStopsToOne = true;
                    boolean addStopsToTwo = false;
                    if(h1Index < h2Index){ //ha az első szakaszhatár előbb van mint a második
                        List<Stop> splitLineOneStops = new ArrayList<>(); //Első vonal stopjai
                        List<Stop> splitLineTwoStops = new ArrayList<>(); //Második vonal stopjai
                        List<Stop> stopstoRemove = new ArrayList<>(); 
                        int lineId = replacementLineCount(model)+1;
                        //Létrehozzuk a potlóvonalakat
                        Line splitLineOne = new Line(lineId,line.getName() + "-1",line.getColor(),false,splitLineOneStops);
                        lineId++;
                        Line splitLineTwo = new Line(lineId,line.getName() + "-2",line.getColor(),false,splitLineTwoStops);
                        for(Stop stop : stops)
                             {
                               //Az első vonalhoz addig adunk hozzá amig el nem érjük az első szakaszhatár megállót, aztán nem
                                if(addStopsToOne)
                                {
                                    Stop newStop = new Stop(stop.getStation(),splitLineOne);
                                    stop.getStation().getStops().add(newStop);
                                    splitLineOne.getStops().add(newStop);
                                    stopstoRemove.add(stop);
                                
                                }
                                //Itt ha elértük az első szakaszhatárt akkor nem adunk az első vonalhoz több megállót
                                if(stop.getStation().equals(h1)) addStopsToOne = false;
                                //Ha elértük a második szakaszhatár megállót akkor kezdünk a második vonalhoz adni megállókat
                                if(stop.getStation().equals(h2)) addStopsToTwo = true;
                                if(addStopsToTwo)
                                {
                                    Stop newStop = new Stop(stop.getStation(),splitLineTwo);
                                    stop.getStation().getStops().add(newStop);
                                    splitLineTwo.getStops().add(newStop);
                                    stopstoRemove.add(stop);
                                
                                }
                                
                            }
                            //Eltávolítjuk azokat a stoppokat a megállóból amik az eredeti vonalhoz tartoztak különben duplikálódnak (eredeti vonal stop és potló vonal stop is benne lesz)
                            stopstoRemove(stopstoRemove);
                            stopstoRemove(line.getStops());
                            //Ha a vonalnak több mint 2 megállója van akkor hozzáadjuk a térképhez
                            if(splitLineOne.getStops().size() >= 2) model.lines.add(splitLineOne);
                            if(splitLineTwo.getStops().size() >= 2) model.lines.add(splitLineTwo);
                            //Eltávolitjuk az eredeti vonalat a térképről
                            model.lines.remove(line);

                    }
                    else //A második szakaszhatár megálló van hamarabb nem az első
                    {   
                        
                        List<Stop> splitLineOneStops = new ArrayList<>();
                        List<Stop> splitLineTwoStops = new ArrayList<>();
                        List<Stop> stopstoRemove = new ArrayList<>();
                        int lineId = replacementLineCount(model)+1;
                        //Létrehozzuk a potlóvonalakat
                        Line splitLineOne = new Line(lineId,line.getName() + "-2",line.getColor(),false,splitLineOneStops);
                        lineId++;
                        Line splitLineTwo = new Line(lineId,line.getName() + "-1",line.getColor(),false,splitLineTwoStops);
                        for(Stop stop : stops)
                        {
                            if(addStopsToOne)
                            {
                                Stop newStop = new Stop(stop.getStation(),splitLineOne);
                                stop.getStation().getStops().add(newStop);
                                splitLineOne.getStops().add(newStop);
                                stopstoRemove.add(stop);
                    
                            }
                            //Ha elértük a második szakaszhatárt akkor nem adunk több megállót az első vonalhoz
                            if(stop.getStation().equals(h2)) addStopsToOne = false;
                            //Ha elértük az első szakaszhatárt akkor kezdünk a második vonalhoz adni megállókat
                            if(stop.getStation().equals(h1)) addStopsToTwo = true;
                            if(addStopsToTwo)
                                {
                                    Stop newStop = new Stop(stop.getStation(),splitLineTwo);
                                    stop.getStation().getStops().add(newStop);
                                    splitLineTwo.getStops().add(newStop);
                                    stopstoRemove.add(stop);
                                }
                               
                        }
                        //Eltávolítjuk azokat a stoppokat a megállóból amik az eredeti vonalhoz tartoztak különben duplikálódnak (eredeti vonal stop és potló vonal stop is benne lesz)
                        stopstoRemove(stopstoRemove);
                        stopstoRemove(line.getStops());
                        model.lines.add(splitLineOne);
                        model.lines.add(splitLineTwo);
                        model.lines.remove(line);
                    }
                }       
               
            }
        }
            
        }
    }
    public static void stopstoRemove(List<Stop> stops){
        for(Stop stop : stops){
            stop.getStation().getStops().remove(stop);
          
        }
    }
    public static boolean linesHaveEnoughStops(List<Line> lines)
    {
        for(Line line : lines){
            if(line.getStops().size() < 2) return false;
        }
        return true;
    }
    public static void removeOldStops(List<Stop> stops,Line line)
    {
        for(Stop stop : stops)
        {
            stop.getStation().getStops().remove(stop);
            line.getStops().remove(stop);
        } 
    }
    public static boolean isSelectionValid(List<Station> stations, List<Line> lines)
    {
        //Megnézzük hogy a kiválasztott megállók egymás után jönnek e
        return (stations.size() > 1 && !lines.isEmpty() && areSelectedStationsConsecutive(stations,lines));
    }
    
    public static boolean isEndStation(Station boundary, List<Stop> stops){
         //Megegyezik e a szakaszhatár a végállomásokkal, ha igen akkor melyikkel
        return stops.getFirst().getStation().getName().equals(boundary.getName()) || stops.getLast().getStation().getName().equals(boundary.getName());
    }
    public static void createAlternativeService(ModelData model, Station stationA, Station stationB) {

        //Csinálunk egy potlóvonalat, és az A és B megállókhoz adjuk őket
        int rlineCount = replacementLineCount(model); 
        Line replacementLine = null;
        String lineName = "P-" + (rlineCount+1);
        replacementLine = new Line(rlineCount+1,lineName,"#009EE3", false, List.of(new Stop(stationA,replacementLine),new Stop(stationB,replacementLine)));
        stationA.getStops().add(new Stop(stationA,replacementLine));
        stationB.getStops().add(new Stop(stationB,replacementLine));
        model.lines.add(replacementLine);

    }
    public static int getIndexOfBoundary(List<Stop> stops,Station boundary) 
    {
        //Hányadik megálló a szakaszhatár a vonalon
        return stops.indexOf(stops.stream().filter(stop -> stop.getStation().equals(boundary)).findFirst().orElse(null));
    }
    public static int replacementLineCount(ModelData model)
    {
        //A line nevéhez szükséges megnézni hogy hány P-vel kezdődő vonal létezik a térképen, és ezt inkrementálni minden egyes új vonalnál
        int rlineCount = 0;
        for(Line line : model.lines){
            if(line.getName().startsWith("P")) rlineCount++;
        }
        return rlineCount;
    }
  
    public static boolean isStationAfterInAnyLine(Station station, Station afterStation, List<Line> lines) {
        return lines.stream().anyMatch(line -> {
            List<Stop> stops = line.getStops();
            int afterStationIdx = -1;
            int stationIdx = -1;
    
            
            for (int i = 0; i < stops.size(); i++) {
                //Megnézzük hogy a következő megálló a vonalon megegyezik-e a kiválasztott megállók listájának a következő elemével
                if (stops.get(i).getStation().equals(afterStation)) {
                    afterStationIdx = i;
                }
                //Megnézzük hogy az előző megálló a vonalon megegyezik-e a kiválasztott megállók listájának a következő elemével
                if (stops.get(i).getStation().equals(station)) {
                    stationIdx = i;
                }
            }
    
            //Ha az egyik teljesül akkor igazat fogunk visszaadni azaz egymás után jön az éppen két vizsgált megálló
            if (afterStationIdx != -1 && stationIdx != -1) {
                return afterStationIdx + 1 == stationIdx || afterStationIdx == stationIdx + 1; 
            }
    
            return false; 
        });
    }
    
    public static boolean areSelectedStationsConsecutive(List<Station> stations, List<Line> lines) {
        //A felső függvény segitségével a kiválasztott megállók mindegyikét megnézzük
        for (int i = 0; i < stations.size() - 1; i++) {
            Station currentStation = stations.get(i);
            Station nextStation = stations.get(i + 1);
            
            //Ha van olyan ami nem sorban jön akkor hamis
            if (!isStationAfterInAnyLine(currentStation, nextStation, lines)) {
                return false;
            }
        }
        //Akkor igaz ha az összes kiválasztott megálló sorban jön
        return true;
    }
}

