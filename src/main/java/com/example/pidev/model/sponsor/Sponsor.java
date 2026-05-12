package com.example.pidev.model.sponsor;

public class Sponsor {

    private int id;
    private int event_id;
    private String company_name;
    private String logo_url;
    private double contribution_name;
    private String contact_email;
    private String contract_url;

    public Sponsor() {}

    public Sponsor(int id, int event_id, String company_name, String logo_url,
                   double contribution_name, String contact_email, String contract_url) {
        this.id = id;
        this.event_id = event_id;
        this.company_name = company_name;
        this.logo_url = logo_url;
        this.contribution_name = contribution_name;
        this.contact_email = contact_email;
        this.contract_url = contract_url;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEvent_id() { return event_id; }
    public void setEvent_id(int event_id) { this.event_id = event_id; }

    public String getCompany_name() { return company_name; }
    public void setCompany_name(String company_name) { this.company_name = company_name; }

    public String getLogo_url() { return logo_url; }
    public void setLogo_url(String logo_url) { this.logo_url = logo_url; }

    public double getContribution_name() { return contribution_name; }
    public void setContribution_name(double contribution_name) { this.contribution_name = contribution_name; }

    public String getContact_email() { return contact_email; }
    public void setContact_email(String contact_email) { this.contact_email = contact_email; }


    public String getContract_url() { return contract_url; }
    private Integer user_id;
    private String access_code;

    public Integer getUser_id() { return user_id; }
    public void setUser_id(Integer user_id) { this.user_id = user_id; }

    public String getAccess_code() { return access_code; }
    public void setAccess_code(String access_code) { this.access_code = access_code; }

    public void setContract_url(String contract_url) { this.contract_url = contract_url; }
}
