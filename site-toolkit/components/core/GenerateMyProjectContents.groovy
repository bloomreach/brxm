
project.log 'Generating MyProject example contents for unit testing ...'

def texenTask = project.createTask('texen')

texenTask.contextProperties = properties['texenContextProperties']
def baseTemplateDir = new File(properties['texenTemplatePath'])
def baseOutputDirectory = new File(properties['texenOutputDirectory'])

project.log "Texen context properties: $texenTask.contextProperties"
project.log "Texen template dir: $baseTemplateDir"
project.log "Texen output dir: $baseOutputDirectory"

project.references.contentFiles.each {
  def itemName = it.name
  def templateFile = new File(baseTemplateDir, itemName)
  def outputName = itemName.replace(/__rootArtifactId__/, texenTask.contextProperties['rootArtifactId'])
  def outputFile = new File(baseOutputDirectory, outputName)
  
  if (!outputFile.isDirectory())
    outputFile.parentFile.mkdirs()
  
  texenTask.templatePath = templateFile.parentFile.canonicalPath
  texenTask.controlTemplate = templateFile.name
  texenTask.outputDirectory = outputFile.parentFile
  texenTask.outputFile = outputFile.name
  
  texenTask.execute()
}

project.log 'Done. Generated MyProject example contents for unit testing ...'
