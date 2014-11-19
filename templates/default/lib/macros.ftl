<#macro generateArtifactTable>
    <#list sleuthkitCase.blackboardArtifactTypesInUse as artifactType>
        <div id="${artifactType}" class="tab-pane">
           <div class="row placeholders">
              <h2 class="sub-header">${artifactType.displayName}</h2>
                <div class="table-responsive">
                  <table class="table table-striped">
                    <thead>
                      <tr>
                        
                        <#list sleuthkitCase.getBlackboardArtifacts(artifactType)?first.attributes as attributes_row>
                         <th>${attributes_row.attributeTypeDisplayName}</th>
                        </#list>
                      </tr>
                    </thead>
                    <tbody>
                     <#list sleuthkitCase.getBlackboardArtifacts(artifactType) as artifact_row>
                      <tr>
                        <#list artifact_row.attributes as attribute>
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