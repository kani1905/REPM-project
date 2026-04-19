package com.repm.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.repm.backend.entity.EnergyData;
import com.repm.backend.entity.User;
import com.repm.backend.entity.Notification;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
public class PdfReportService {

    // private static final String LOGO_PATH = "C:\\Users\\kanik\\.gemini\\antigravity\\brain\\cdb5cb09-eb80-469d-be5d-7a1b4cda7900\\repm_logo_professional_1775274721949.png";

    public ByteArrayInputStream generateUserReport(User user, List<EnergyData> data, List<Notification> notifications, String rangeLabel) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Logo and Header
            /* 
            try {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(LOGO_PATH);
                logo.scaleToFit(80, 80);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
            } catch (Exception e) {
            }
            */

            // Fonts
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(30, 60, 114));
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font fontFooter = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);

            // Title
            Paragraph title = new Paragraph(rangeLabel + " Performance Report", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph subtitle = new Paragraph("Renewable Energy Performance Monitor (REPM)", fontSubtitle);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(new Paragraph(" "));

            // User Info Section
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10f);
            
            addInfoCell(infoTable, "User Details:", fontHeader, new Color(30, 60, 114));
            addInfoCell(infoTable, "Report Metadata:", fontHeader, new Color(30, 60, 114));
            
            addInfoCell(infoTable, "Name: " + (user.getFullName() != null ? user.getFullName() : "User") + "\nEmail: " + (user.getEmail() != null ? user.getEmail() : "N/A"), fontNormal, Color.LIGHT_GRAY);
            addInfoCell(infoTable, "Generated On: " + LocalDate.now() + "\nMetric: Energy Utilization", fontNormal, Color.LIGHT_GRAY);
            
            document.add(infoTable);
            document.add(new Paragraph(" "));

            // Summary Stats Section
            List<EnergyData> filteredData = data.stream()
                    .filter(d -> d.getEnergyProduced() != null && d.getEnergyProduced() > 0)
                    .toList();

            // If everything is zero, we still want to show the original data in the table, 
            // but calculations should be based on what's available.
            // Actually, let's use the filtered list for calculations.
            
            double totalProduced = filteredData.stream().mapToDouble(EnergyData::getEnergyProduced).sum();
            double totalConsumed = filteredData.stream().mapToDouble(EnergyData::getEnergyConsumed).sum();
            double totalCo2Saved = filteredData.stream().mapToDouble(EnergyData::getCo2Consumed).sum();
            double avgEff = filteredData.stream().mapToDouble(EnergyData::getEfficiency).average().orElse(0.0);

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(15f);

            addCell(summaryTable, "Total Produced (kWh)", fontHeader, new Color(16, 185, 129)); 
            addCell(summaryTable, "Total Consumed (kWh)", fontHeader, new Color(239, 68, 68)); 
            addCell(summaryTable, "CO2 Saved (kg)", fontHeader, new Color(20, 184, 166)); 
            addCell(summaryTable, "Avg Efficiency (%)", fontHeader, new Color(59, 130, 246)); 

            addCell(summaryTable, String.format("%.2f", totalProduced), fontNormal, Color.LIGHT_GRAY);
            addCell(summaryTable, String.format("%.2f", totalConsumed), fontNormal, Color.LIGHT_GRAY);
            addCell(summaryTable, String.format("%.2f", totalCo2Saved), fontNormal, Color.LIGHT_GRAY);
            addCell(summaryTable, String.format("%.2f%%", avgEff), fontNormal, Color.LIGHT_GRAY);

            document.add(summaryTable);
            document.add(new Paragraph(" "));

            // Detailed Data Table
            Paragraph dataTitle = new Paragraph("Detailed Performance Logs", fontSubtitle);
            dataTitle.setSpacingBefore(10f);
            document.add(dataTitle);
            
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setWidths(new float[]{2, 2, 2, 2, 2, 2});

            addCell(table, "Date", fontHeader, Color.DARK_GRAY);
            addCell(table, "Source", fontHeader, Color.DARK_GRAY);
            addCell(table, "Produced", fontHeader, Color.DARK_GRAY);
            addCell(table, "Consumed", fontHeader, Color.DARK_GRAY);
            addCell(table, "Efficiency", fontHeader, Color.DARK_GRAY);
            addCell(table, "CO2 Saved", fontHeader, Color.DARK_GRAY);

            // Use original data for the table but skip zeros if there's at least one real entry
            List<EnergyData> tableData = filteredData.isEmpty() ? data : filteredData;

            for (EnergyData d : tableData) {
                addCell(table, d.getEntryDate() != null ? d.getEntryDate().toString() : "N/A", fontNormal, Color.LIGHT_GRAY);
                addCell(table, d.getSource() != null ? d.getSource() : "N/A", fontNormal, Color.LIGHT_GRAY);
                addCell(table, String.format("%.2f", d.getEnergyProduced() != null ? d.getEnergyProduced() : 0), fontNormal, Color.LIGHT_GRAY);
                addCell(table, String.format("%.2f", d.getEnergyConsumed() != null ? d.getEnergyConsumed() : 0), fontNormal, Color.LIGHT_GRAY);
                addCell(table, String.format("%.1f%%", d.getEfficiency() != null ? d.getEfficiency() : 0), fontNormal, Color.LIGHT_GRAY);
                addCell(table, String.format("%.2f", d.getCo2Consumed() != null ? d.getCo2Consumed() : 0), fontNormal, Color.LIGHT_GRAY);
            }

            document.add(table);
            
            // Recent Notifications Section
            if (notifications != null && !notifications.isEmpty()) {
                document.add(new Paragraph(" "));
                Paragraph notifTitle = new Paragraph("Recent System Alerts & Notifications", fontSubtitle);
                notifTitle.setSpacingBefore(10f);
                document.add(notifTitle);

                PdfPTable notifTable = new PdfPTable(3);
                notifTable.setWidthPercentage(100);
                notifTable.setSpacingBefore(10f);
                notifTable.setWidths(new float[]{2, 6, 2});

                addCell(notifTable, "Type", fontHeader, Color.DARK_GRAY);
                addCell(notifTable, "Message", fontHeader, Color.DARK_GRAY);
                addCell(notifTable, "Date", fontHeader, Color.DARK_GRAY);

                // Show only last 10 notifications
                List<Notification> latestNotifs = notifications.stream()
                        .sorted((n1, n2) -> n2.getTimestamp().compareTo(n1.getTimestamp()))
                        .limit(10)
                        .toList();

                for (Notification n : latestNotifs) {
                    addCell(notifTable, n.getType(), fontNormal, Color.LIGHT_GRAY);
                    addCell(notifTable, n.getMessage(), fontNormal, Color.LIGHT_GRAY);
                    addCell(notifTable, n.getTimestamp().toLocalDate().toString(), fontNormal, Color.LIGHT_GRAY);
                }
                document.add(notifTable);
            }
            
            Paragraph footer = new Paragraph("\n\nThis is a confidential report generated for " + (user.getFullName() != null ? user.getFullName() : "User") + ".\n© 2026 REPM Monitor - Promoting Sustainable Energy.", fontFooter);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateAdminReport(List<User> users, List<EnergyData> allData, String rangeLabel) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Logo
            /*
            try {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(LOGO_PATH);
                logo.scaleToFit(80, 80);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
            } catch (Exception e) {
            }
            */

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(30, 60, 114));
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font fontFooter = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);

            Paragraph title = new Paragraph("System-Wide " + rangeLabel + " Report", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Administrative Analytics Overview", fontSubtitle);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(new Paragraph(" "));

            // Metadata
            document.add(new Paragraph("Total Registered Users: " + users.size(), fontSubtitle));
            document.add(new Paragraph("Generation Date: " + LocalDate.now(), fontNormal));
            document.add(new Paragraph(" "));

            // Aggregated Summary
            List<EnergyData> filteredAllData = allData.stream()
                    .filter(d -> d.getEnergyProduced() != null && d.getEnergyProduced() > 0)
                    .toList();

            double totalProduced = filteredAllData.stream().mapToDouble(EnergyData::getEnergyProduced).sum();
            double totalConsumed = filteredAllData.stream().mapToDouble(EnergyData::getEnergyConsumed).sum();
            double avgEff = filteredAllData.stream().mapToDouble(EnergyData::getEfficiency).average().orElse(0.0);

            PdfPTable adminSummary = new PdfPTable(3);
            adminSummary.setWidthPercentage(100);
            addCell(adminSummary, "Total Energy Produced (kWh)", fontHeader, new Color(30, 60, 114));
            addCell(adminSummary, "Total Energy Consumed (kWh)", fontHeader, new Color(30, 60, 114));
            addCell(adminSummary, "Overall Avg Efficiency (%)", fontHeader, new Color(30, 60, 114));

            addCell(adminSummary, String.format("%.2f", totalProduced), fontNormal, Color.LIGHT_GRAY);
            addCell(adminSummary, String.format("%.2f", totalConsumed), fontNormal, Color.LIGHT_GRAY);
            addCell(adminSummary, String.format("%.2f%%", avgEff), fontNormal, Color.LIGHT_GRAY);

            document.add(adminSummary);
            document.add(new Paragraph(" "));

            // User Performance Table
            Paragraph userTableTitle = new Paragraph("User Performance Breakdown", fontSubtitle);
            userTableTitle.setSpacingBefore(10f);
            document.add(userTableTitle);

            PdfPTable userTable = new PdfPTable(4);
            userTable.setWidthPercentage(100);
            userTable.setSpacingBefore(10f);

            addCell(userTable, "Full Name", fontHeader, Color.DARK_GRAY);
            addCell(userTable, "Email", fontHeader, Color.DARK_GRAY);
            addCell(userTable, "Data Points", fontHeader, Color.DARK_GRAY);
            addCell(userTable, "Efficiency Avg", fontHeader, Color.DARK_GRAY);

            for (User u : users) {
                long count = filteredAllData.stream().filter(d -> d.getUser() != null && d.getUser().getId().equals(u.getId())).count();
                double userAvgEff = filteredAllData.stream()
                        .filter(d -> d.getUser() != null && d.getUser().getId().equals(u.getId()))
                        .mapToDouble(d -> d.getEfficiency() != null ? d.getEfficiency() : 0)
                        .average().orElse(0.0);

                if (count == 0)
                    continue; // Only show active users in the range report

                addCell(userTable, (u.getFullName() != null ? u.getFullName() : "User"), fontNormal, Color.LIGHT_GRAY);
                addCell(userTable, (u.getEmail() != null ? u.getEmail() : "N/A"), fontNormal, Color.LIGHT_GRAY);
                addCell(userTable, String.valueOf(count), fontNormal, Color.LIGHT_GRAY);
                addCell(userTable, String.format("%.2f%%", userAvgEff), fontNormal, Color.LIGHT_GRAY);
            }

            document.add(userTable);

            // Overall Analytics Summary
            document.add(new Paragraph("\nSummary Insight:", fontSubtitle));
            document.add(new Paragraph("The system is currently operating at an average efficiency of " + String.format("%.2f%%", avgEff) +
                    ". Administrative oversight is recommended for users falling below 50% efficiency.", fontNormal));

            // Footer
            Paragraph footer = new Paragraph("\n\nOfficial Admin System Report - Generated by REPM Engine", fontFooter);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addCell(PdfPTable table, String text, Font font, Color backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(backgroundColor);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addInfoCell(PdfPTable table, String text, Font font, Color backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(backgroundColor);
        cell.setPadding(10);
        cell.setBorderColor(Color.WHITE);
        table.addCell(cell);
    }
}
