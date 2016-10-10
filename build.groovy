import org.freecompany.redline.Builder
import org.freecompany.redline.header.Architecture
import org.freecompany.redline.header.Os
import org.freecompany.redline.header.RpmType
import org.freecompany.redline.payload.Directive
import tablesaw.*
import tablesaw.rules.DirectoryRule
import tablesaw.rules.Rule
import tablesaw.rules.SimpleRule


programName = saw.getProperty("program_name")
version = saw.getProperty("version")
release = "1"

rpmFile = "$programName-$version-${release}.rpm"
srcRpmFile = "$programName-$version-${release}.src.rpm"

rpmDir = "build/rpm"
new DirectoryRule("build")
rpmDirRule = new DirectoryRule(rpmDir)

//------------------------------------------------------------------------------
//Build rpm file
rpmBaseInstallDir = "/opt/$programName"
rpmRule = new SimpleRule("package-rpm").setDescription("Build RPM Package")
		.addDepend(rpmDirRule)
		.addTarget("$rpmDir/$rpmFile")
		.setMakeAction("doRPM")

//Files to be installed
installFiles = new RegExFileSet(saw.getProperty("source_files"), ".*").recurse()

def doRPM(Rule rule)
{
	//Build rpm using redline rpm library
	host = InetAddress.getLocalHost().getHostName()
	rpmBuilder = new Builder()
	rpmBuilder.with
			{
				description = saw.getProperty("description")
				group = saw.getProperty("group")
				license = saw.getProperty("license")
				setPackage(programName, version, release)
				setPlatform(Architecture.NOARCH, Os.LINUX)
				summary = saw.getProperty("summary")
				type = RpmType.BINARY
				url = saw.getProperty("url")
				vendor = saw.getProperty("vendor")
				provides = programName
				buildHost = host
				sourceRpm = srcRpmFile
			}
			
	rpmBuilder.setPrefixes(rpmBaseInstallDir)

	//Adding dependencies
	rpmBuilder.addDependencyMore("jre", saw.getProperty("jre_version"))

	addFileSetToRPM(rpmBuilder, rpmBaseInstallDir, installFiles)


	println("Building RPM "+rule.getTarget())
	outputFile = new FileOutputStream(rule.getTarget())
	rpmBuilder.build(outputFile.channel)
	outputFile.close()
}

//A helper method for adding the contents of each FileSet to the rpm
def addFileSetToRPM(Builder builder, String destination, AbstractFileSet files)
{
	for (AbstractFileSet.File file : files.getFiles())
	{
		File f = new File(file.getBaseDir(), file.getFile())
		if (file.getFile().startsWith("bin"))
		{
			builder.addFile(destination + "/" +file.getFile(), f, 0755)
		}
		else if (file.getFile().startsWith("conf"))
		{
			builder.addFile(destination + "/" + file.getFile(), f, 0644, new Directive(Directive.RPMFILE_CONFIG | Directive.RPMFILE_NOREPLACE))
		}
		else
		{
			builder.addFile(destination + "/" + file.getFile(), f)
		}
	}
}

uploadRule = new SimpleRule("upload-rpm").setDescription("Upload rpm to Nexus repository")
		.addDepend(rpmRule)
		.setMakeAction("doUpload")
		.alwaysRun()
		
def doUpload(Rule rule)
{
	rpmFile = rpmRule.getTarget()
	nexusUser = saw.getProperty("nexus_user")
	nexusPassword = saw.getProperty("nexus_password")
	nexusBaseUrl = saw.getProperty("nexus_base_url")
	cmd = "curl -v -u $nexusUser:$nexusPassword --upload-file $rpmFile $nexusBaseUrl/apache/$programName/$version/$programName-$version"+".rpm"
	
	saw.exec(cmd)
}


saw.setDefaultTarget("package-rpm")