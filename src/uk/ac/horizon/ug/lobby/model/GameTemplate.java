/**
 * 
 */
package uk.ac.horizon.ug.lobby.model;

import javax.persistence.Entity;
import javax.persistence.Id;
/**
 * @author cmg
 *
 */
@Entity
public class GameTemplate {
	/** globally unique (random) ID and primary key */
	@Id
	private String id;
	/** title */
	private String title;
	/** description */
	private String description;
	/** (default) language code */
	private String lang;
	/**
	 */
	public GameTemplate() {
		super();
	}
	/**
	 * @param id
	 */
	public GameTemplate(String id) {
		super();
		this.id = id;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the lang
	 */
	public String getLang() {
		return lang;
	}
	/**
	 * @param lang the lang to set
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}
	
}
