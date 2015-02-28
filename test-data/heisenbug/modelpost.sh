# load project
curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" http://localhost:8080/alfresco/service/workspaces/master/sites/ems-test/projects?createSite=true -d '{"elements":[{"name":"OpenMBEE Test","sysmlid":"PROJECT-_24_3be8fa68-7b04-442c-a88e-f0f5cbad5c1a","specialization":{"type":"Project"}}]}'

# load the model
curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" http://localhost:8080/alfresco/service/workspaces/master/sites/ems-test/elements -d @model.json

# load branch... this is may not be necessary
#curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" http://localhost:8080/alfresco/service/workspaces/master/sites/ems-test/projects -d '{"elements":[{"name":"OpenMBEE Test","sysmlid":"PROJECT-_24_3be8fa68-7b04-442c-a88e-f0f5cbad5c1a","specialization":{"type":"Project"}}]}'

# load view
#curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" http://localhost:8080/alfresco/service/workspaces/master/sites/ems-test/elements -d @view.json

# load product
#curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" http://localhost:8080/alfresco/service/workspaces/master/sites/ems-test/elements -d '{"elements":[{"sysmlid":"_24_0_5_1_8660276_1411498411582_887504_15890","specialization":{"view2view":[{"childrenViews":[],"id":"_24_MMS_1413410023260_ed84382e-167d-47c3-868b-38d2304c599f"},{"childrenViews":[],"id":"_24_0_5_1_8660276_1411498416458_259170_15981"},{"childrenViews":[],"id":"_24_MMS_1414446916597_fb68d60e-4c68-4816-94ee-b01ec4348ea8"},{"childrenViews":[],"id":"_24_0_5_1_8660276_1411498416459_702337_15982"},{"childrenViews":["_24_0_5_1_8660276_1411498416458_259170_15981","_24_0_5_1_8660276_1411498416459_702337_15982","_24_0_5_1_8660276_1411498416459_638278_15983","_24_MMS_1413395721244_80a65535-4715-4547-bd90-48fff1949b44","_24_MMS_1414446916597_fb68d60e-4c68-4816-94ee-b01ec4348ea8"],"id":"_24_0_5_1_8660276_1411498411582_887504_15890"},{"childrenViews":[],"id":"_24_0_5_1_8660276_1411498416459_638278_15983"},{"childrenViews":[],"id":"_24_MMS_1413410030804_a8c38d88-a1b0-4de6-92fb-a2799c9dfaa1"},{"childrenViews":["_24_MMS_1413410030804_a8c38d88-a1b0-4de6-92fb-a2799c9dfaa1","_24_MMS_1413410023260_ed84382e-167d-47c3-868b-38d2304c599f"],"id":"_24_MMS_1413395721244_80a65535-4715-4547-bd90-48fff1949b44"}],"noSections":[],"type":"Product"}}]}'

# add association e.g., new element in view - THIS IS ENOUGH TO START HEISENBUG
#curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" http://localhost:8080/alfresco/service/workspaces/master/sites/ems-test/elements -d '{"elements":[{"name":"","owner":"_24_0_5_1_8660276_1411498473933_452352_21092","documentation":"","sysmlid":"_24_0_5_1_f4e036d_1421884149943_907605_13828","specialization":{"targetAggregation":"NONE","source":"_24_0_5_1_f4e036d_1421884149944_417514_13829","sourceAggregation":"COMPOSITE","target":"_24_0_5_1_f4e036d_1421884149944_41389_13830","type":"Association","ownedEnd":["_24_0_5_1_f4e036d_1421884149944_41389_13830"]}},{"name":"","owner":"_24_0_5_1_f4e036d_1421884149943_907605_13828","documentation":"","sysmlid":"_24_0_5_1_f4e036d_1421884149944_41389_13830","specialization":{"propertyType":"_24_0_5_1_8660276_1411498411582_887504_15890","isSlot":false,"isDerived":false,"value":[],"type":"Property"}},{"name":"sacrificial View","owner":"_24_0_5_1_8660276_1411498411582_887504_15890","documentation":"","sysmlid":"_24_0_5_1_f4e036d_1421884149944_417514_13829","specialization":{"propertyType":"_24_0_5_1_8660276_1413409371591_465936_16215","isSlot":false,"isDerived":false,"value":[],"type":"Property"}}]}'

# add property of view for association
#curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" http://localhost:8080/alfresco/service/workspaces/master/sites/ems-test/elements -d '{"elements":[{"name":"sacrificial View","owner":"_24_0_5_1_8660276_1411498411582_887504_15890","sysmlid":"_24_0_5_1_f4e036d_1421884149944_417514_13829"}]}'

# re-export the view model hierarchy 
#curl -w "\n%{http_code}\n" -u admin:admin -X POST -H "Content-Type:application/json" http://localhost:8080/alfresco/service/workspaces/master/sites/ems-test/elements -d '{"elements":[{"name":"","sysmlid":"_24_0_5_1_f4e036d_1421884673682_302428_13856","owner":"_24_0_5_1_8660276_1411498473933_452352_21092","documentation":"","specialization":{"targetAggregation":"NONE","source":"_24_0_5_1_f4e036d_1421884673682_863881_13857","sourceAggregation":"COMPOSITE","target":"_24_0_5_1_f4e036d_1421884673682_723210_13858","type":"Association","ownedEnd":["_24_0_5_1_f4e036d_1421884673682_723210_13858"]}},{"name":"sacrificial view","sysmlid":"_24_0_5_1_f4e036d_1421884673682_863881_13857","owner":"_24_0_5_1_8660276_1411498411582_887504_15890","documentation":"","specialization":{"propertyType":"_24_0_5_1_8660276_1413409371591_465936_16215","isSlot":false,"isDerived":false,"value":[],"type":"Property"}},{"name":"","sysmlid":"_24_0_5_1_f4e036d_1421884673682_723210_13858","owner":"_24_0_5_1_f4e036d_1421884673682_302428_13856","documentation":"","specialization":{"propertyType":"_24_0_5_1_8660276_1411498411582_887504_15890","isSlot":false,"isDerived":false,"value":[],"type":"Property"}},{"owner":"_24_0_5_1_8660276_1411498411582_887504_15890","sysmlid":"_24_0_5_1_8660276_1413408730337_79604_16179"},{"owner":"_24_0_5_1_8660276_1411498411582_887504_15890","sysmlid":"_24_0_5_1_8660276_1411498416458_796909_15980"},{"owner":"_24_MMS_1413395721244_80a65535-4715-4547-bd90-48fff1949b44","sysmlid":"_24_0_5_1_8660276_1413417880299_99034_16301"},{"owner":"_24_MMS_1413395721244_80a65535-4715-4547-bd90-48fff1949b44","sysmlid":"_24_0_5_1_8660276_1413417880301_549766_16305"},{"owner":"_24_0_5_1_8660276_1411498411582_887504_15890","sysmlid":"_24_0_5_1_8660276_1411498416458_34281_15979"},{"owner":"_24_0_5_1_8660276_1411498411582_887504_15890","sysmlid":"_24_0_5_1_8660276_1414446925841_771668_16706"},{"owner":"_24_0_5_1_8660276_1411498411582_887504_15890","sysmlid":"_24_0_5_1_8660276_1411498416458_99530_15978"}]}'

