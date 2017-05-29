package com.owsega.c2ghackathon;

/**
 * A user of the web app.
 *
 * @author Seyi Owoeye. Created on 5/29/17.
 */
public class User {
    private String uid;
    private int status;
    private String firstName;
    private String lastName;
    private String address;
    private String dateOfBirth;
    private String email;
    private String profilePicUrl;
    private String occupation;

    User(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public int getStatus() {
        return status;
    }

    public User setStatus(int status) {
        this.status = status;
        return this;
    }

    public User setStatus(Status status) {
        this.status = status.ordinal();
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public User setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public User setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public User setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public User setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
        return this;
    }

    public String getOccupation() {
        return occupation;
    }

    public User setOccupation(String occupation) {
        this.occupation = occupation;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public User setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public enum Status {REGISTRANT, REVIEWER, PROCESSOR}
}
