/*
 * Copyright (c) 2010 Red Hat, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see 
 * <http://www.gnu.org/licenses>.
 */

package com.redhat.rcm.version.util;

import static com.redhat.rcm.version.testutil.TestProjectUtils.getResourceFile;
import static com.redhat.rcm.version.testutil.TestProjectUtils.loadModel;
import static com.redhat.rcm.version.testutil.TestProjectUtils.newVersionManagerSession;
import static com.redhat.rcm.version.util.PomUtils.writeModifiedPom;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.apache.maven.mae.project.key.VersionlessProjectKey;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import com.redhat.rcm.version.mgr.session.VersionManagerSession;

import java.io.File;
import java.util.List;

public class PomUtilsTest
{

    private static final String BASE = "pom-formats/";

    // private static final String TOOLCHAIN = BASE + "toolchain.pom";

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public final TestName testName = new TestName();

    private File reports;

    private File workspace;

    private VersionManagerSession session;

    @Before
    public void setup()
        throws Exception
    {
        workspace = tempFolder.newFolder( "workspace" );
        reports = tempFolder.newFolder( "reports" );

        // File toolchainPom = getResourceFile( TOOLCHAIN );
        // Model toolchainModel = loadModel( toolchainPom );
        // MavenProject toolchainProject = new MavenProject( toolchainModel );

        session = newVersionManagerSession( workspace, reports, null );
        // session.setToolchain( toolchainPom, toolchainProject );
    }

    @Test
    public void pomRewritePreservesXMLAttributesInPluginConfiguration()
        throws Exception
    {
        File pom = getResourceFile( BASE + "plugin-config-attributes.pom" );
        final File temp = tempFolder.newFile( pom.getName() );
        copyFile( pom, temp );
        pom = temp;

        final Model model = loadModel( pom );

        assertThat( model.getDependencies(), notNullValue() );
        assertThat( model.getDependencies().size(), equalTo( 1 ) );
        for ( final Dependency dep : model.getDependencies() )
        {
            System.out.println( "Verifying starting condition for dep: " + dep );
            assertThat( dep.getVersion(), notNullValue() );
        }

        assertThat( model.getBuild(), notNullValue() );

        List<Plugin> plugins = model.getBuild().getPlugins();
        assertThat( plugins, notNullValue() );
        assertThat( plugins.size(), equalTo( 1 ) );

        Plugin plugin = plugins.get( 0 );
        Object config = plugin.getConfiguration();
        assertThat( config, notNullValue() );

        assertThat( config.toString().contains( "<delete dir=\"foobar\"" ), equalTo( true ) );

        model.getDependencies().get( 0 ).setVersion( null );
        plugin.setVersion( null );

        final VersionlessProjectKey coord = new VersionlessProjectKey( model.getGroupId(), model.getArtifactId() );

        final File basedir = tempFolder.newFolder( testName.getMethodName() + ".out.dir" );
        final File out = writeModifiedPom( model, pom, coord, model.getVersion(), basedir, session, false );

        final String pomStr = readFileToString( out );
        System.out.println( "Modified POM for " + testName.getMethodName() + ":\n\n" + pomStr + "\n\n" );

        final Model changed = loadModel( out );

        assertThat( changed.getDependencies(), notNullValue() );
        assertThat( changed.getDependencies().size(), equalTo( 1 ) );
        for ( final Dependency dep : changed.getDependencies() )
        {
            assertThat( dep.getVersion(), nullValue() );
        }

        assertThat( changed.getBuild(), notNullValue() );

        plugins = changed.getBuild().getPlugins();
        assertThat( plugins, notNullValue() );
        assertThat( plugins.size(), equalTo( 1 ) );

        plugin = plugins.get( 0 );
        config = plugin.getConfiguration();
        assertThat( config, notNullValue() );
        assertThat( config.toString().contains( "<delete dir=\"foobar\"" ), equalTo( true ) );
    }

}
