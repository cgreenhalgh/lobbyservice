/**
 * Copyright 2010 The University of Nottingham
 * 
 * This file is part of lobbyservice.
 *
 *  lobbyservice is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  lobbyservice is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with lobbyservice.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package uk.ac.horizon.ug.lobby.protocol;

/** Location constraint, e.g. for game lookup.
 * Initially just a circle.
 * 
 * @author cmg
 *
 */
public class LocationConstraint {
	/** location constraint type - required */
	private LocationConstraintType type;
	/** latitude E6 - optional, depending on type [CIRCLE] */
	private Integer latitudeE6;
	/** longitude E6 - optional, depending on type [CIRCLE] */
	private Integer longitudeE6;
	/** radius (metres) - optional, depending on type [CIRCLE] */
	private Float radiusMetres;
	/** cons */
	public LocationConstraint() {		
	}
	/**
	 * @return the type
	 */
	public LocationConstraintType getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(LocationConstraintType type) {
		this.type = type;
	}
	/**
	 * @return the latitudeE6
	 */
	public Integer getLatitudeE6() {
		return latitudeE6;
	}
	/**
	 * @param latitudeE6 the latitudeE6 to set
	 */
	public void setLatitudeE6(Integer latitudeE6) {
		this.latitudeE6 = latitudeE6;
	}
	/**
	 * @return the longitudeE6
	 */
	public Integer getLongitudeE6() {
		return longitudeE6;
	}
	/**
	 * @param longitudeE6 the longitudeE6 to set
	 */
	public void setLongitudeE6(Integer longitudeE6) {
		this.longitudeE6 = longitudeE6;
	}
	/**
	 * @return the radiusMetres
	 */
	public Float getRadiusMetres() {
		return radiusMetres;
	}
	/**
	 * @param radiusMetres the radiusMetres to set
	 */
	public void setRadiusMetres(Float radiusMetres) {
		this.radiusMetres = radiusMetres;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LocationConstraint [latitudeE6=" + latitudeE6
				+ ", longitudeE6=" + longitudeE6 + ", radiusMetres="
				+ radiusMetres + ", type=" + type + "]";
	}
	
}
