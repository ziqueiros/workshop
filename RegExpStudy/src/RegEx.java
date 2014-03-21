
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.lang.System.*;

public class RegEx {

	private Vector<Mapping> vectorMapping;

	static public class Mapping{
		String description;
		Pattern pattern;
		String queuename;
		
		public Mapping(String desc,String pattern,String queue){
			this.description = desc;
			this.pattern = Pattern.compile(pattern);
			this.queuename = queue;
		}
		
		@Override
		public String toString(){
			return description +" "+ queuename +" "+ pattern.toString();
		}
		
	}
	
	public static void main(String... args) throws SQLException{
		RegEx r = new RegEx();
		r.constructHashmap();

		//If you have to test against several java expresion you jave to consider to create more general
		//expresions
		Mapping m = r.checkPatternFileMatching("CO-GENCO-DLY.COH1200PFS12_0_27_Prt-02-20-2014.run0");
		
		out.println(m);
		
	}
	
	private void constructHashmap() throws SQLException{
		
		vectorMapping = new Vector<Mapping>();
		
		Mapping tempMapping = new Mapping("0","CO-GENCO-DLY\\.COH1200[\\w_-]+\\.run[\\d]+","queue");
		vectorMapping.add(tempMapping);
		tempMapping = new Mapping("1","CO-GENCO-DLY\\.COH1200A[\\w_-]+\\.run[\\d]+","queue");
		vectorMapping.add(tempMapping);
		tempMapping = new Mapping("2","CO-GENCO-DLY\\.COH1200EZ[\\w_-]+\\.run[\\d]+","queue");
		vectorMapping.add(tempMapping);
		tempMapping = new Mapping("3","CO-GENCO-DLY\\.COH1200PFS[\\w_-]+\\.run[\\d]+","queue");
		vectorMapping.add(tempMapping);
		
	}
	
	private Mapping checkPatternFileMatching(String fileName) {
		Mapping entry = null;
		boolean matchesFileName = false;
		Iterator<Mapping> it = vectorMapping.iterator();		

		while (it.hasNext()) {
			entry = (Mapping)it.next();	

			Matcher m = entry.pattern.matcher(fileName);
			matchesFileName = m.matches();

			// Verify if this fileName matches de filename
			if (matchesFileName) {
				return entry;
			}

		}
		return null;

	}
	
}
