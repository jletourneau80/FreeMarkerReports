<#import "/default/layout/artifact_attributes.ftl" as artifact_attribute_info>
<#import "/default/layout/content_tag_attributes.ftl" as content_tag_attributes_info>

<#macro generateArtifactTable>
    <#list artifacts?keys as artifactType>
        <div id="${artifactType}" class="tab-pane">
           <div class="row placeholders">
              <h2 class="sub-header">${artifactDisplayMapping[artifactType]}</h2>
                <div class="table-responsive">
                  <table class="table table-striped">
                    <thead>
                      <tr>
                        <#list artifact_attribute_info.artifact_attributes[artifactType]![] as first_artifact_row>
                         <th>${first_artifact_row}</th>
                        </#list>
                      </tr>
                    </thead>
                    <tbody>
                     <#list artifacts[artifactType] as artifact_row>
                      <tr>
                        <#list artifact_attribute_info.artifact_attributes[artifactType]![] as attribute>
                            <td>${artifact_row[attribute]!"empty"}</td>
                        </#list>
                      </tr>
                      </#list>
                    </tbody>
                  </table>
                </div>
                </div>
            </div>
        </#list>
        
</#macro>


<#macro generateContentTagTable>
    <#list contentTags?keys as contentTagName>
        <div id="${contentTagName}" class="tab-pane">
           <div class="row placeholders">
              <h2 class="sub-header">${contentTags[contentTagName]?first.name.displayName}</h2>
                <div class="table-responsive">
                  <table class="table table-striped">
                    <thead>
                      <tr>
                        <#list content_tag_attributes_info.content_tag_attributes![] as first_artifact_row>
                         <th>${first_artifact_row}</th>
                        </#list>
                      </tr>
                    </thead>
                    <tbody>
                     <#list contentTags[contentTagName] as artifact_row>
                      <tr>
                            <td>${artifact_row.content.uniquePath!"empty"}</td>
                            <td>${artifact_row.comment!""}</td>
                            <#list artifact_row.content.genInfoArtifact.attributes![] as attribute>
                                <td>${attribute.valueString}</td>
                            </#list>
                           
                      </tr>
                      </#list>
                    </tbody>
                  </table>
                </div>
                </div>
            </div>
        </#list>
        
</#macro>