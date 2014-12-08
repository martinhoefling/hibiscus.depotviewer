package de.open4me.depot.abruf.hbci;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import de.willuhn.annotation.Lifecycle;
import de.willuhn.annotation.Lifecycle.Type;
import de.willuhn.jameica.hbci.SynchronizeOptions;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.KontoType;
import de.willuhn.jameica.hbci.synchronize.hbci.HBCISynchronizeBackend;
import de.willuhn.jameica.hbci.synchronize.hbci.HBCISynchronizeJobProvider;
import de.willuhn.jameica.hbci.synchronize.jobs.SynchronizeJob;
import de.willuhn.jameica.hbci.synchronize.jobs.SynchronizeJobKontoauszug;
import de.willuhn.logging.Logger;

/**
 * Implementierung eines Job-Providers fuer das Abrufen des Kontoauszuges fuer Depots..
 */
@Lifecycle(Type.CONTEXT)
public class DVHBCISynchronizeJobProviderDepotKontoauszug implements HBCISynchronizeJobProvider
{
  @Resource
  private HBCISynchronizeBackend backend = null;

  private final static List<Class<? extends SynchronizeJob>> JOBS = new ArrayList<Class<? extends SynchronizeJob>>()
  {{
    add(DVHBCISynchronizeJobDepotKontoauszug.class);
  }};

  // Die vom Job-Provider unterstuetzten Konto-Arten
  private final static Set<KontoType> SUPPORTED = new HashSet<KontoType>()
  {{
    add(KontoType.FONDSDEPOT);
    add(KontoType.WERTPAPIERDEPOT);
  }};
  

  /**
   * @see de.willuhn.jameica.hbci.synchronize.SynchronizeJobProvider#getSynchronizeJobs(de.willuhn.jameica.hbci.rmi.Konto)
   */
  public List<SynchronizeJob> getSynchronizeJobs(Konto k)
  {
    List<SynchronizeJob> jobs = new LinkedList<SynchronizeJob>();
    
    for (Konto kt:backend.getSynchronizeKonten(k))
    {
      try
      {
    	  if (!supports(null, kt)) {
    		  continue;
    	  }
        final SynchronizeOptions options = new SynchronizeOptions(kt);

        // Sync-Option nicht aktiv
        // Also nichts zu tun.
        if (!options.getSyncKontoauszuege())
          continue;
        
        SynchronizeJobKontoauszug job = backend.create(SynchronizeJobKontoauszug.class,kt);
        job.setContext(SynchronizeJob.CTX_ENTITY,kt);
        jobs.add(job);
      }
      catch (Exception e)
      {
        Logger.error("unable to load synchronize jobs",e);
      }
    }

    return jobs;
  }
  
  
  /**
   * @see de.willuhn.jameica.hbci.synchronize.SynchronizeJobProvider#supports(java.lang.Class, de.willuhn.jameica.hbci.rmi.Konto)
   */
  @Override
  public boolean supports(Class<? extends SynchronizeJob> type, Konto k)
  {
    // Kein Konto angegeben. Dann gehen wir mal davon aus, dass es nicht geht
    if (k == null)
      return false;
    
    KontoType kt = null;
    try
    {
      // Checken, ob das vielleicht ein nicht unterstuetztes Konto ist
      kt = KontoType.find(k.getAccountType());

      // Wenn kein konkreter Typ angegeben ist, dann unterstuetzen wir es nicht
      if (kt == null)
        return false;

      // Ansonsten dann, wenn er in supported ist
      return SUPPORTED.contains(kt);
    }
    catch (RemoteException re)
    {
      Logger.error("unable to determine support for account-type " + kt,re);
    }
    return false;
  }
  
  /**
   * @see de.willuhn.jameica.hbci.synchronize.SynchronizeJobProvider#getJobTypes()
   */
  public List<Class<? extends SynchronizeJob>> getJobTypes()
  {
    return JOBS;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o)
  {
    // Umsaetze und Salden werden zum Schluss ausgefuehrt,
    // damit die oben gesendeten Ueberweisungen gleich mit
    // erscheinen, insofern die Bank das unterstuetzt.
    return 1;
  }
}