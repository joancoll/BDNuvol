package cat.dam.andy.bdnuvol;

import androidx.annotation.NonNull;

public class User {
    private String name;
    private String lastname;

    //constructor
    //és necessari explicitar un constructor public sense arguments
    public User(String name, String lastname) {
        this.name = name;
        this.lastname = lastname;
    }

    //getters i setters
    public String getName() {
        return name;
    }
    public String getLastname() {
        return lastname;
    }
    public void setName(String name) {this.name = name;}
    public void setLastname(String lastname) {this.lastname = lastname;}

    @NonNull
    public String toString(){
    //Important fer mètode .toString per alimentar correctament Spinner o ListView
    //ja que és el mètode que es crida automàticament per representar els objectes.
        return (name + " "+ lastname);
    }
}