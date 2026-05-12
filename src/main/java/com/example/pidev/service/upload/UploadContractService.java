package com.example.pidev.service.upload;

import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.sponsor.ContractPdfGenerator;
import com.example.pidev.service.sponsor.SponsorService;

import java.io.File;

public class UploadContractService {

    private final CloudinaryUploadService cloud = new CloudinaryUploadService();
    private final SponsorService sponsorService = new SponsorService();

    // ✅ exactement ce nom (pour ton controller)
    public String generateAndUploadContract(Sponsor s) {
        if (s == null) throw new IllegalArgumentException("Sponsor null");
        if (s.getId() <= 0) throw new IllegalArgumentException("Sponsor doit avoir un ID (en DB) avant contrat");

        File pdf = ContractPdfGenerator.generate(s);
        try {
            String url = cloud.uploadPdfContract(pdf);

            // ✅ save dans DB
            sponsorService.updateContractUrl(s.getId(), url);

            return url;
        } catch (Exception e) {
            throw new RuntimeException("Upload contract failed: " + e.getMessage(), e);
        }
    }
}
