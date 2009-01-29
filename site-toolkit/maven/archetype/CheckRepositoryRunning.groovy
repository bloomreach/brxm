import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepository;

println "Checking the embedded repository running"

repositoryRunning = false
repository = null
session = null

while (!repositoryRunning)
{
    Thread.sleep(3000)
    
    try
    {
        if (repository == null)
            repository = HippoRepositoryFactory.getHippoRepository("${properties['repository.address']}")
        
        session = repository.login("${properties['repository.username']}", "${properties['repository.password']}".toCharArray())
        
        if (session != null)
            repositoryRunning = true
    }
    catch (e)
    {
        println "Not validated yet, still checking. The current error message: " + e.getMessage()
    }
}

println ""

if (repositoryRunning)
    println "The embedded repository is running!"
else
    println "The embedded repository is not running!"

