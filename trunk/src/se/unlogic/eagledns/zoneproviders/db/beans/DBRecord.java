package se.unlogic.eagledns.zoneproviders.db.beans;

import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Tokenizer;
import org.xbill.DNS.Type;
import org.xbill.DNS.Tokenizer.Token;

import se.unlogic.utils.dao.annotations.DAOPopulate;
import se.unlogic.utils.dao.annotations.ManyToOne;
import se.unlogic.utils.dao.annotations.PrimaryKey;
import se.unlogic.utils.dao.annotations.Table;
import se.unlogic.utils.xml.Elementable;
import se.unlogic.utils.xml.XMLElement;
import se.unlogic.utils.xml.XMLGenerator;

@XMLElement
@Table(name="records")
public class DBRecord implements Elementable {

	@DAOPopulate
	@PrimaryKey(autoGenerated = true)
	@XMLElement
	private Integer recordID;

	@DAOPopulate(columnName = "zoneID")
	@ManyToOne
	@XMLElement
	private DBZone zone;

	@DAOPopulate
	@XMLElement
	private String name;

	@DAOPopulate
	@XMLElement
	private String type;

	@DAOPopulate
	@XMLElement	
	private String dclass;	
	
	@DAOPopulate
	@XMLElement
	private String content;

	@DAOPopulate
	@XMLElement
	private Integer ttl;

	@DAOPopulate
	@XMLElement
	private Integer prio;

	public Integer getRecordID() {

		return recordID;
	}

	public void setRecordID(Integer recordID) {

		this.recordID = recordID;
	}

	public String getName() {

		return name;
	}

	public void setName(String name) {

		this.name = name;
	}

	public String getType() {

		return type;
	}

	public void setType(String type) {

		this.type = type;
	}

	public String getContent() {

		return content;
	}

	public void setContent(String content) {

		this.content = content;
	}

	public Integer getTtl() {

		return ttl;
	}

	public void setTtl(Integer ttl) {

		this.ttl = ttl;
	}

	public Integer getPrio() {

		return prio;
	}

	public void setPrio(Integer prio) {

		this.prio = prio;
	}

	public Element toXML(Document doc) {

		return XMLGenerator.toXML(this, doc);
	}

	public DBZone getZone() {

		return zone;
	}

	public void setZone(DBZone zone) {

		this.zone = zone;
	}

	public Record getRecord(long zoneTTL, Name origin) throws TextParseException, IOException {

		long ttl;
		
		if(this.ttl == null){
			
			ttl = zoneTTL;
			
		}else{
			
			ttl = this.ttl;
		}
		
		String rdata;
		
		if(this.prio != null){
			
			rdata = this.prio + " " + this.content;
			
		}else{
			
			rdata = content;
		}
		
		Name name;
		
		if(this.name.equals("@")){
			
			name = origin;
			
		}else{
			
			name = Name.fromString(this.name);
		}
		
		Tokenizer tokenizer = new Tokenizer(rdata);
		
		Token foo = tokenizer.get();
		
		return Record.fromString(name, Type.value(type), DClass.value(dclass), ttl, tokenizer, origin);
	}

	
	public String getDclass() {
	
		return dclass;
	}

	
	public void setDclass(String dclass) {
	
		this.dclass = dclass;
	}
}
