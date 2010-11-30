/*
 *  Copyright (C) 2010 John Casey.
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.redhat.rcm.version;

import org.commonjava.emb.EMBException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ExampleMode;
import org.kohsuke.args4j.Option;

import com.redhat.rcm.version.mgr.VersionManager;
import com.redhat.rcm.version.mgr.VersionManagerSession;

import java.io.File;

public class Cli
{
    @Argument( index = 0, metaVar = "target", usage = "POM file (or directory containing POM files) to modify." )
    private File target;

    @Argument( index = 1, metaVar = "BOM", usage = "Bill-of-Materials POM file supplying versions." )
    private File bom;

    @Option( name = "-p", usage = "POM path pattern (glob)" )
    private String pomPattern = "**/*.pom";

    @Option( name = "-D", usage = "Don't rename directories" )
    private final boolean preserveDirs = false;

    @Option( name = "-b", usage = "Backup original files here up before modifying." )
    private File backups;

    @Option( name = "-r", usage = "Write reports here." )
    private final File reports = new File( "vman-reports" );

    @Option( name = "-h", usage = "Print this message and quit" )
    private boolean help = false;

    public static void main( final String[] args )
        throws EMBException
    {
        final Cli cli = new Cli();
        final CmdLineParser parser = new CmdLineParser( cli );
        try
        {
            parser.parseArgument( args );

            if ( cli.help )
            {
                printUsage( parser, null );
            }
            else if ( cli.target == null || cli.bom == null )
            {
                printUsage( parser, null );
            }
            else
            {
                cli.run();
            }
        }
        catch ( final CmdLineException error )
        {
            printUsage( parser, error );
        }
    }

    public Cli( final File target, final File bom )
    {
        this.target = target;
        this.bom = bom;
    }

    public Cli()
    {
    }

    public void run()
        throws EMBException
    {
        final VersionManager vman = new VersionManager();
        final VersionManagerSession session = new VersionManagerSession( backups, preserveDirs );
        if ( target.isDirectory() )
        {
            vman.modifyVersions( target, pomPattern, bom, session );
        }
        else
        {
            vman.modifyVersions( bom, bom, session );
        }

        reports.mkdirs();
        vman.generateReports( reports, session );
    }

    private static void printUsage( final CmdLineParser parser, final CmdLineException error )
    {
        if ( error != null )
        {
            System.err.println( "Invalid option(s): " + error.getMessage() );
            System.err.println();
        }

        parser.printUsage( System.err );
        System.err.println( "Usage: $0 [OPTIONS] <target-path> <BOM-path>" );
        System.err.println();
        System.err.println();
        System.err.println( parser.printExample( ExampleMode.ALL ) );
        System.err.println();
    }

    protected String getPomPattern()
    {
        return pomPattern;
    }

    protected void setPomPattern( final String pomPattern )
    {
        this.pomPattern = pomPattern;
    }

    protected boolean isHelp()
    {
        return help;
    }

    protected void setHelp( final boolean help )
    {
        this.help = help;
    }

}
