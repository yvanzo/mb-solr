package org.musicbrainz.search.solrwriter;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public abstract class MBXMLWriterTest extends SolrTestCaseJ4{
	public static String corename;
	public static ArrayList<String> doc;
	public static final String uuid = "fff523fa-f2db-40eb-be81-f4544b43f39c";

	@BeforeClass
	public static void beforeClass() throws Exception {
		initCore("solrconfig.xml", "schema.xml", "mbsssss", corename);
	}

	/**
	 * Add a document containing doc to the current core.
	 * @param withStore whether the _store field should be populated with data
	 * @throws IOException
	 */
	public void addDocument(boolean withStore) throws IOException {
		ArrayList<String> values = new ArrayList<>(doc);
		if (withStore) {
			String xmlfilepath = MBXMLWriterTest.class.getResource(corename + ".xml").getFile();
			byte[] content = Files.readAllBytes(Paths.get(xmlfilepath));
			String xml = new String(content);

			values.add(0, xml);
			values.add(0, "_store");
		}

		assertU(adoc((values.toArray(new String[values.size()]))));
		assertU(commit());
	}


	@After
	public void After () {
		clearIndex();
	}

	@Test
	public void performCoreTest() throws Exception {
		addDocument(true);
		String xmlfilepath;
		byte[] content;
		String xml;

		xmlfilepath = MBXMLWriterTest.class.getResource(corename + "-list.xml").getFile();
		content = Files.readAllBytes(Paths.get(xmlfilepath));
		xml = new String(content);

		String response = h.query(req("q", "*:*", "fl", "score", "wt", "mbxml"));
		Source test = Input.fromMemory(response).build();

		Diff d = DiffBuilder.compare(Input.fromMemory(xml)).withTest(test).build();
		assertFalse(d.hasDifferences());
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	/**
	 * Check that a useful error message is shown to the user if 'score' is not in the field list.
	 */
	public void testNoScoreException() throws Exception {
		addDocument(true);
		thrown.expectMessage(MBXMLWriter.SCORE_NOT_IN_FIELD_LIST);
		h.query(req("q", "*:*", "wt", "mbxml"));
	}

	@Test
	/**
	 * Check that a useful error message is shown to the user if the document doesn't have a '_store' field.
	 */
	public void testNoStoreException() throws Exception {
		addDocument(false);
		thrown.expectMessage(MBXMLWriter.NO_STORE_VALUE);
		h.query(req("q", "*:*", "fl", "score", "wt", "mbxml"));
	}
}
