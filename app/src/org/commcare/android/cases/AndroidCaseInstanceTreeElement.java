/**
 * 
 */
package org.commcare.android.cases;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.android.database.SqlStorage;
import org.commcare.android.database.SqlStorageIterator;
import org.commcare.android.database.user.models.ACase;
import org.commcare.android.database.user.models.CaseIndexTable;
import org.commcare.cases.instance.CaseChildElement;
import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.CacheHost;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.DataUtil;

/**
 * @author ctsims
 *
 */
public class AndroidCaseInstanceTreeElement extends CaseInstanceTreeElement implements CacheHost {
    SqlStorageIterator<ACase> iter;
    CaseIndexTable mCaseIndexTable;
    
    protected Hashtable<Integer, Integer> multiplicityIdMapping = new Hashtable<Integer, Integer>();
    
    public AndroidCaseInstanceTreeElement(AbstractTreeElement instanceRoot, SqlStorage<ACase> storage, boolean reportMode) {
        super(instanceRoot, storage, reportMode);
        mCaseIndexTable = new CaseIndexTable();
    }
    
    
    protected synchronized void getCases() {
        if(cases != null) {
            return;
        }
        objectIdMapping = new Hashtable<Integer, Integer>();
        cases = new Vector<CaseChildElement>();
        System.out.println("Getting Cases!");
        long timeInMillis = System.currentTimeMillis();

        int mult = 0;

        for(IStorageIterator i = ((SqlStorage<ACase>)storage).iterate(false); i.hasMore();) {
            int id = i.nextID();
            cases.addElement(new CaseChildElement(this, id, null, mult));
            objectIdMapping.put(DataUtil.integer(id), DataUtil.integer(mult));
            multiplicityIdMapping.put(DataUtil.integer(mult), DataUtil.integer(id));
            mult++;
        }
        long value = System.currentTimeMillis() - timeInMillis;
        System.out.println("Case iterate took: " + value + "ms");
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.commcare.cases.util.StorageBackedTreeRoot#union(java.util.Vector, java.util.Vector)
     */
    @Override
    protected Vector<Integer> union(Vector<Integer> selectedCases, Vector<Integer> cases) {
        //This is kind of (ok, so really) awkward looking, but we can't use sets in 
        //ccj2me (Thanks, Nokia!) also, there's no _collections_ interface in
        //j2me (thanks Sun!) so this is what we get.
        HashSet<Integer> selected = new HashSet<Integer>(selectedCases);
        selected.addAll(selectedCases);
        
        HashSet<Integer> other = new HashSet<Integer>();
        other.addAll(cases);
        
        selected.retainAll(other);
        
        selectedCases.clear();
        selectedCases.addAll(selected);
        return selectedCases;
    }
    
    @Override
    protected Vector<Integer> getNextIndexMatch(Vector<String> keys, Vector<Object> values, IStorageUtilityIndexed<?> storage) {
        String firstKey = keys.elementAt(0);
        
        //If the index object starts with "case_in" it's actually a case index query and we need to run
        //this over the case index table
        if(firstKey.startsWith(Case.INDEX_CASE_INDEX_PRE)) {
            String indexName = firstKey.substring(Case.INDEX_CASE_INDEX_PRE.length());
            Vector <Integer> matchingCases = mCaseIndexTable.getCasesMatchingIndex(indexName, (String)values.elementAt(0));
            
            //Clear the most recent index and wipe it, because there is no way it is going to be useful
            //after this
            mMostRecentBatchFetch = new String[2][];
            
            //remove the match from the inputs
            keys.removeElementAt(0);
            values.removeElementAt(0);
            return matchingCases;
        }
        
        //Otherwise see how many of these we can bulk process
        int numKeys;
        for(numKeys = 0 ; numKeys < keys.size() ; ++numKeys) {
            //If the current key is an index fetch, we actually can't do it in bulk,
            //so we need to stop
            if(keys.elementAt(numKeys).startsWith(Case.INDEX_CASE_INDEX_PRE)) {
                break;
            }
            //otherwise, it's now in our queue
        }
        
        SqlStorage<ACase> sqlStorage = ((SqlStorage<ACase>)storage);
        String[] namesToMatch = new String[numKeys];
        String[] valuesToMatch = new String[numKeys];
        for(int i = numKeys -1 ; i >= 0; i--) {
            namesToMatch[i] = keys.elementAt(i);
            valuesToMatch[i] = (String)values.elementAt(i);
        }
        mMostRecentBatchFetch = new String[2][];
        mMostRecentBatchFetch[0] = namesToMatch;
        mMostRecentBatchFetch[1] = valuesToMatch;
        
        Vector<Integer> ids = sqlStorage.getIDsForValues(namesToMatch, valuesToMatch);
        
        //Ok, we matched! Remove all of the keys that we matched
        for(int i = 0 ; i < numKeys ; ++i) {
            keys.removeElementAt(0);
            values.removeElementAt(0);
        }
        return ids;
    }
    
    
    public String getCacheIndex(TreeReference ref) {
        //NOTE: there's no evaluation here as to whether the ref is suitable
        //we only follow one pattern for now and it's evaluated below. 
        
        getCases();
        
        //Testing - Don't bother actually seeing whether this fits
        int i = ref.getMultiplicity(1);
        if(i != -1 ) {
            Integer val = this.multiplicityIdMapping.get(DataUtil.integer(i));
            if(val == null) { 
                return null;
            }
            else { return val.toString();}
        }
        return null;
    }


    @Override
    public boolean isReferencePatternCachable(TreeReference ref) {
        //we only support one pattern here, a raw, qualified
        //reference to an element at the case level with no
        //predicate support. The ref basically has to be a raw
        //pointer to one of this instance's children
        if(!ref.isAbsolute()) {
            return false;
        }
        
        if(ref.hasPredicates()) { return false; }
        if(ref.size() != 2) { return false; }
        
        if(!"casedb".equalsIgnoreCase(ref.getName(0))) { return false; }
        if(!"case".equalsIgnoreCase(ref.getName(1))) { return false;}
        if(ref.getMultiplicity(1) < 0) { return false;}
         
        return true;
    }
    
    String[][] mMostRecentBatchFetch = null;
    
    /*
     * ](non-Javadoc)
     * @see org.javarosa.core.model.utils.CacheHost#guessCachePrimer()
     */
    @Override
    public String[][] getCachePrimeGuess() { 
        return mMostRecentBatchFetch;
    }

}
