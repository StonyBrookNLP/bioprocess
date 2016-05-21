def loadJar(jarFile):
    '''load a jar at runtime using the system Classloader (needed for JDBC)
    adapted from http://forum.java.sun.com/thread.jspa?threadID=300557
    Author: Steve (SG) Langer Jan 2007 translated the above Java to Jython
    Reference: https://wiki.python.org/jython/JythonMonthly/Articles/January2007/3
    Author: seansummers@gmail.com simplified and updated for jython-2.5.3b3+
    >>> loadJar('jtds-1.3.1.jar')
    >>> from java import lang, sql
    >>> lang.Class.forName('net.sourceforge.jtds.jdbc.Driver')
    <type 'net.sourceforge.jtds.jdbc.Driver'>
    >>> sql.DriverManager.getDriver('jdbc:jtds://server')
    jTDS 1.3.1
    '''
    from java import io, net, lang
    u = io.File(jarFile).toURL() if type(jarFile) <> net.URL else jarFile
    m = net.URLClassLoader.getDeclaredMethod('addURL', [net.URL])
    m.accessible = 1
    m.invoke(lang.ClassLoader.getSystemClassLoader(), [u])

import java.lang.reflect.Method 
import java.lang.ClassLoader as javaClassLoader 
from java.lang import Object as javaObject 
from java.io import File as javaFile 
from java.net import URL as javaURL 
from java.net import URLClassLoader 
import jarray 

class classPathHacker(object): 
    """Original Author: SG Langer Jan 2007, conversion from Java to Jython 
    Updated version (supports Jython 2.5.2) >From http://glasblog.1durch0.de/?p=846 
    
    Purpose: Allow runtime additions of new Class/jars either from 
    local files or URL 
    """ 
        
    def addFile(self, s): 
        """Purpose: If adding a file/jar call this first 
        with s = path_to_jar""" 
        # make a URL out of 's' 
        f = javaFile(s) 
        u = f.toURL() 
        a = self.addURL(u) 
        return a 
      
    def addURL(self, u): 
         """Purpose: Call this with u= URL for 
         the new Class/jar to be loaded""" 
         sysloader = javaClassLoader.getSystemClassLoader() 
         sysclass = URLClassLoader 
         method = sysclass.getDeclaredMethod("addURL", [javaURL]) 
         a = method.setAccessible(1) 
         jar_a = jarray.array([u], javaObject) 
         b = method.invoke(sysloader, [u]) 
         return u 

class classPathHackerBroken :
##########################################################
# from http://forum.java.sun.com/thread.jspa?threadID=300557
#
# Author: SG Langer Jan 2007 translated the above Java to this
#       Jython class
# Purpose: Allow runtime additions of new Class/jars either from
#       local files or URL
######################################################
    import java.lang.reflect.Method
    import java.io.File
    import java.net.URL
    import java.net.URLClassLoader
    import jarray

    def addFile (self, s):
        #############################################
        # Purpose: If adding a file/jar call this first
        #       with s = path_to_jar
        #############################################

        # make a URL out of 's'
        f = self.java.io.File (s)
        u = f.toURL ()
        a = self.addURL (u)
        return a

    def addURL (self, u):
        ##################################
        # Purpose: Call this with u= URL for
        #       the new Class/jar to be loaded
        #################################

        parameters = self.jarray.array([self.java.net.URL], self.java.lang.Class)
        sysloader =  self.java.lang.ClassLoader.getSystemClassLoader()
        sysclass = self.java.net.URLClassLoader
        method = sysclass.getDeclaredMethod("addURL", parameters)
        a = method.setAccessible(1)
        jar_a = self.jarray.array([u], self.java.lang.Object)
        b = method.invoke(sysloader, jar_a)
        return u
