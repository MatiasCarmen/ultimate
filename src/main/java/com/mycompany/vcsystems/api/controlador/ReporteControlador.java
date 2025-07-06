package com.mycompany.vcsystems.api.controlador;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.borders.SolidBorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.mycompany.vcsystems.modelo.service.IncidenciaService;
import com.mycompany.vcsystems.modelo.entidades.Incidencia;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/reportes")


/**
 *
 * @author MatiasCarmen
 */

public class ReporteControlador {

    @Autowired
    private IncidenciaService incidenciaService;

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportarIncidenciasPDF() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            addMetadata(pdf);
            addContent(document);

            document.close();

            byte[] pdfBytes = baos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "incidencias.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private void addMetadata(PdfDocument pdf) {
        pdf.getDocumentInfo().setTitle("Reporte de Incidencias");
        pdf.getDocumentInfo().setAuthor("VCSystems");
        pdf.getDocumentInfo().setCreator("VCSystems");
    }

    private void addContent(Document document) {
        // Título
        Paragraph title = new Paragraph("Reporte de Incidencias")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));

        // Tabla de incidencias
        float[] columnWidths = {1, 3, 2, 2, 2};
        Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

        // Encabezados
        addTableHeader(table);

        // Datos
        List<Incidencia> incidencias = incidenciaService.listAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Incidencia inc : incidencias) {
            table.addCell(new Cell().add(new Paragraph(inc.getIdIncidencia().toString())));
            table.addCell(new Cell().add(new Paragraph(inc.getCliente().getNombreEmpresa())));
            table.addCell(new Cell().add(new Paragraph(inc.getTecnico() != null ? inc.getTecnico().getNombre() : "Sin asignar")));
            table.addCell(new Cell().add(new Paragraph(inc.getEstado().toString())));
            table.addCell(new Cell().add(new Paragraph(inc.getCreadoAt().format(formatter))));
        }

        document.add(table);
    }

    private void addTableHeader(Table table) {
        Stream.of("ID", "Cliente", "Técnico", "Estado", "Fecha")
            .forEach(columnTitle -> {
                Cell header = new Cell();
                header.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                header.setBorder(new SolidBorder(2f));
                header.add(new Paragraph(columnTitle));
                header.setTextAlignment(TextAlignment.CENTER);
                header.setVerticalAlignment(VerticalAlignment.MIDDLE);
                table.addCell(header);
            });
    }
}
