/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.report.freemarkerreport;

import javax.swing.JPanel;

import org.openide.util.NbBundle;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.datamodel.*;
import org.sleuthkit.autopsy.report.GeneralReportModule;
import org.sleuthkit.autopsy.report.ReportProgressPanel;
import org.sleuthkit.autopsy.ingest.IngestManager;
import org.sleuthkit.datamodel.BlackboardArtifact;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Template;
import java.nio.file.Files;
import org.sleuthkit.autopsy.coreutils.PlatformUtil;
import java.util.HashMap;
import java.util.ArrayList;
import org.sleuthkit.autopsy.report.GeneralReportModule;
import org.sleuthkit.autopsy.report.ReportProgressPanel;

/**
 * Generates a report file based on template file.
 */
class FreeMarkerReportModule implements GeneralReportModule {

    private static final Logger logger = Logger.getLogger(FreeMarkerReportModule.class.getName());
    private static FreeMarkerReportModule instance = null;
    private Case currentCase;
    private SleuthkitCase skCase;
    private String reportPath;
    private String templatesLocation = PlatformUtil.getUserDirectory().getAbsolutePath() + "\\templates";

    private Configuration cfg;
    // Hidden constructor for the report
    private FreeMarkerReportModule() {
        try{
            // Create your Configuration instance, and specify if up to what FreeMarker
            // version (here 2.3.21) do you want to apply the fixes that are not 100%
            // backward-compatible. See the Configuration JavaDoc for details.
            cfg = new Configuration(Configuration.VERSION_2_3_21);

            // Specify the source where the template files come from. Here I set a
            // plain directory for it, but non-file-system sources are possible too:
            logger.log(Level.INFO,"Looking for templates in: " + templatesLocation);
            cfg.setDirectoryForTemplateLoading(new File(templatesLocation));

            // Set the preferred charset template files are stored in. UTF-8 is
            // a good choice in most applications:
            cfg.setDefaultEncoding("UTF-8");

            // Sets how errors will appear.
            // During development TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        }catch (Exception e){
            // can't find file
            logger.log(Level.SEVERE, "Can't find template location.");
        }
    }

    // Get the default implementation of this report
    public static synchronized FreeMarkerReportModule getDefault() {
        if (instance == null) {
            instance = new FreeMarkerReportModule();
        }
        return instance;
    }

    /**
     * Generates a body file format report for use with the MAC time tool.
     *
     * @param path path to save the report
     * @param progressPanel panel to update the report's progress
     */
    @Override
    public void generateReport(String path, ReportProgressPanel progressPanel) {

        String selectedTemplate = "default";
        File selectedTemplateDirectory = new File(templatesLocation + "\\" + selectedTemplate);
        File[] filesInTemplateDir = selectedTemplateDirectory.listFiles();
        String reportExtension = ".html";
        for (File f : filesInTemplateDir){
            if (f.getName().contains("report")){
                reportExtension = f.getName().substring(f.getName().lastIndexOf("."));
            }
        }
        
        // Start the progress bar and setup the report
        progressPanel.setIndeterminate(false);
        progressPanel.start();
        progressPanel.updateStatusLabel(NbBundle.getMessage(this.getClass(), "FreeMarkerReportModule.progress.querying"));
        reportPath = path + "FreeMarkerReportOutput"+reportExtension; //NON-NLS
        currentCase = Case.getCurrentCase();
        skCase = currentCase.getSleuthkitCase();

        progressPanel.updateStatusLabel(NbBundle.getMessage(this.getClass(), "FreeMarkerReportModule.progress.loading"));
        // Check if ingest has finished
        String ingestwarning = "";
        if (IngestManager.getInstance().isIngestRunning()) {
            ingestwarning = NbBundle.getMessage(this.getClass(), "FreeMarkerReportModule.ingestWarning.text");
        }
        progressPanel.setMaximumProgress(5);
        progressPanel.increment();

        HashMap data = new HashMap();
        data.put("case", currentCase);
        data.put("sleuthkitCase", skCase);
        
        //<artifactName : ListOf<attribute:value>>
        
        HashMap<String, ArrayList<HashMap<String,String>>> artifacts = new HashMap<String, ArrayList<HashMap<String, String>>>();
        HashMap<String, String> artifactDisplayMapping = new HashMap<String, String>();
        try{
            for (BlackboardArtifact.ARTIFACT_TYPE artifactType : skCase.getBlackboardArtifactTypesInUse()){
                artifacts.put(artifactType.getLabel(), new ArrayList<HashMap<String, String>>());
                artifactDisplayMapping.put(artifactType.getLabel(), artifactType.getDisplayName());
                
                for (BlackboardArtifact artifact : skCase.getBlackboardArtifacts(artifactType)){
                
                    HashMap<String, String> attributes = new HashMap<String, String>();
                    for (BlackboardAttribute attribute : artifact.getAttributes()){
                        attributes.put(attribute.getAttributeTypeDisplayName(), attribute.getValueString());
                    }
                    attributes.put("Source File", skCase.getAbstractFileById(artifact.getObjectID()).getUniquePath());
                    artifacts.get(artifactType.getLabel()).add(attributes);
                }
            }
        }catch(Exception e){
            logger.log(Level.INFO, e.getMessage());
        }
        data.put("artifacts", artifacts);
        data.put("artifactDisplayMapping",artifactDisplayMapping);
        
        
        HashMap<String, ArrayList<ContentTag>> contentTagMap = new HashMap<String, ArrayList<ContentTag>>();
        try{
            for (ContentTag tag : skCase.getAllContentTags()){
                if (!contentTagMap.containsKey(String.valueOf(tag.getName().getId()))){
                    contentTagMap.put(String.valueOf(tag.getName().getId()), new ArrayList<ContentTag>());
                }
                contentTagMap.get(String.valueOf(tag.getName().getId())).add(tag);
            }
        }catch(Exception e){
            logger.log(Level.INFO, e.getMessage());
        }

        data.put("contentTags", contentTagMap);
        
        try{
            Template temp = cfg.getTemplate(selectedTemplate + "\\report" + reportExtension);
            BufferedWriter out = new BufferedWriter(new FileWriter(reportPath));
            temp.process(data, out);
            File source = new File(templatesLocation+ "\\" + selectedTemplate + "\\assets");
            File dest = new File(path + "\\assets");
            try {
                FileUtils.copyDirectory(source, dest);
            } catch (IOException e) {
                logger.log(Level.INFO,e.getMessage());
            }
            
        }catch (Exception e){
            logger.log(Level.SEVERE,e.getMessage());
        }
        progressPanel.complete();
        
        progressPanel.increment();
        progressPanel.complete();
    }

    public static void copyFileUsingStream(AbstractFile file, File jFile) throws IOException {
        InputStream is = new ReadContentInputStream(file);
        OutputStream os = new FileOutputStream(jFile);
        byte[] buffer = new byte[8192];
        int length;
        try {
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }

        } finally {
            is.close();
            os.close();
        }
    }

    @Override
    public String getName() {
        String name = NbBundle.getMessage(this.getClass(), "FreeMarkerReportModule.getName.text");
        return name;
    }

    @Override
    public String getRelativeFilePath() {
        return "FreeMarkerReportModule";
    }

    @Override
    public String getDescription() {
        String desc = NbBundle.getMessage(this.getClass(), "FreeMarkerReportModule.getDesc.text");
        return desc;
    }

    @Override
    public JPanel getConfigurationPanel() {
        return null; // No configuration panel
    }
}
